package com.fireniceinc.ali;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

public class MainActivity extends Activity {

    private static final String ZERO =
        "0000000000000000000000000000000000000000000000000000000000000000";

    private TextView status;

    static class Check {
        boolean ok;
        String reason = "";
        List<String[]> rows = new ArrayList<>();
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        ScrollView scroll = new ScrollView(this);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(40, 72, 40, 48);
        box.setBackgroundColor(Color.rgb(7, 11, 16));

        TextView title = new TextView(this);
        title.setText("ALI");
        title.setTextSize(42);
        title.setTextColor(Color.rgb(255, 107, 53));
        title.setGravity(Gravity.CENTER);

        TextView sub = new TextView(this);
        sub.setText("Architecture B • Continuity Kernel v0.2.0");
        sub.setTextSize(17);
        sub.setTextColor(Color.WHITE);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 18, 0, 28);

        status = new TextView(this);
        status.setTextSize(16);
        status.setTextColor(Color.LTGRAY);
        status.setTextIsSelectable(true);

        box.addView(title);
        box.addView(sub);
        box.addView(status);

        scroll.addView(box);
        setContentView(scroll);

        runTest();
    }

    private void runTest() {
        String id = "unknown";

        try {
            id = identity();

            Check before = verify();

            if (!before.ok) {
                fail(id, before.reason);
                return;
            }

            if (before.rows.isEmpty()) {
                append(
                    1,
                    "GENESIS",
                    "instance=" + id + ";version=0.2.0",
                    ZERO
                );

                before = verify();

                if (!before.ok) {
                    fail(id, before.reason);
                    return;
                }
            }

            String[] head =
                before.rows.get(before.rows.size() - 1);

            append(
                Long.parseLong(head[0]) + 1,
                "BOOT",
                "version=0.2.0",
                head[5]
            );

            Check after = verify();

            if (!after.ok) {
                fail(id, after.reason);
                return;
            }

            long boots = 0;

            for (String[] row : after.rows) {
                if ("BOOT".equals(row[2])) {
                    boots++;
                }
            }

            head = after.rows.get(after.rows.size() - 1);

            status.setTextColor(
                Color.rgb(110, 231, 183)
            );

            status.setText(
                "CONTINUITY: VERIFIED\n\n" +
                "Integrated test: PASS\n" +
                "Write + reread: PASS\n" +
                "Hash chain: PASS\n" +
                "Previous history: PRESERVED\n\n" +
                "Instance: " + shortValue(id) + "\n" +
                "Boot count: " + boots + "\n" +
                "Events: " + after.rows.size() + "\n" +
                "Head: " + shortValue(head[5]) + "\n" +
                "Storage: PRIVATE / LOCAL\n\n" +
                "This launch appended and verified a BOOT event.\n\n" +
                "NEXT: fully close ALI, reopen it, and confirm " +
                "the boot count increases while the instance remains the same."
            );

        } catch (Exception e) {
            fail(
                id,
                e.getClass().getSimpleName() +
                ": " +
                e.getMessage()
            );
        }
    }

    private String identity() throws Exception {

        File f =
            new File(
                getFilesDir(),
                "ali-instance-id.txt"
            );

        if (f.exists()) {
            String s = read(f).trim();

            if (s.isEmpty()) {
                throw new IOException(
                    "Empty identity"
                );
            }

            return s;
        }

        String s =
            UUID.randomUUID().toString();

        write(
            f,
            s + "\n",
            false
        );

        if (!s.equals(read(f).trim())) {
            throw new IOException(
                "Identity round-trip mismatch"
            );
        }

        return s;
    }

    private void append(
        long seq,
        String type,
        String payload,
        String previous
    ) throws Exception {

        String time =
            Instant.now().toString();

        String p64 =
            Base64.getEncoder()
                .encodeToString(
                    payload.getBytes(
                        StandardCharsets.UTF_8
                    )
                );

        String canonical =
            seq + "|" +
            time + "|" +
            type + "|" +
            p64 + "|" +
            previous;

        write(
            new File(
                getFilesDir(),
                "ali-continuity.jsonl"
            ),
            canonical +
                "|" +
                sha(canonical) +
                "\n",
            true
        );
    }

    private Check verify() {

        Check c = new Check();

        File f =
            new File(
                getFilesDir(),
                "ali-continuity.jsonl"
            );

        if (!f.exists()) {
            c.ok = true;
            return c;
        }

        String previous = ZERO;
        long seq = 1;

        try (
            BufferedReader r =
                new BufferedReader(
                    new InputStreamReader(
                        new FileInputStream(f),
                        StandardCharsets.UTF_8
                    )
                )
        ) {

            String line;

            while ((line = r.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] p =
                    line.split("\\|", -1);

                if (p.length != 6) {
                    return bad(
                        c,
                        "Malformed record " + seq
                    );
                }

                if (
                    Long.parseLong(p[0]) != seq
                ) {
                    return bad(
                        c,
                        "Sequence mismatch " + seq
                    );
                }

                if (
                    !previous.equals(p[4])
                ) {
                    return bad(
                        c,
                        "Previous-hash mismatch " +
                        seq
                    );
                }

                String canonical =
                    p[0] + "|" +
                    p[1] + "|" +
                    p[2] + "|" +
                    p[3] + "|" +
                    p[4];

                if (
                    !sha(canonical).equals(p[5])
                ) {
                    return bad(
                        c,
                        "Record-hash mismatch " +
                        seq
                    );
                }

                Base64.getDecoder()
                    .decode(p[3]);

                c.rows.add(p);

                previous = p[5];
                seq++;
            }

            c.ok = true;
            return c;

        } catch (Exception e) {

            return bad(
                c,
                e.getClass().getSimpleName() +
                ": " +
                e.getMessage()
            );
        }
    }

    private Check bad(
        Check c,
        String reason
    ) {
        c.ok = false;
        c.reason = reason;
        return c;
    }

    private void fail(
        String id,
        String reason
    ) {
        status.setTextColor(
            Color.rgb(248, 113, 113)
        );

        status.setText(
            "CONTINUITY: FAILED / REFUSED\n\n" +
            "Instance: " + id + "\n" +
            "Reason: " + reason + "\n\n" +
            "No damaged history was silently replaced."
        );
    }

    private String shortValue(String s) {
        return s.length() > 20
            ? s.substring(0, 20) + "…"
            : s;
    }

    private void write(
        File f,
        String s,
        boolean append
    ) throws Exception {

        try (
            FileOutputStream o =
                new FileOutputStream(
                    f,
                    append
                )
        ) {
            o.write(
                s.getBytes(
                    StandardCharsets.UTF_8
                )
            );

            o.flush();
            o.getFD().sync();
        }
    }

    private String read(
        File f
    ) throws Exception {

        StringBuilder b =
            new StringBuilder();

        try (
            BufferedReader r =
                new BufferedReader(
                    new InputStreamReader(
                        new FileInputStream(f),
                        StandardCharsets.UTF_8
                    )
                )
        ) {

            String s;

            while ((s = r.readLine()) != null) {
                b.append(s).append('\n');
            }
        }

        return b.toString();
    }

    private String sha(
        String s
    ) throws Exception {

        byte[] d =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    s.getBytes(
                        StandardCharsets.UTF_8
                    )
                );

        StringBuilder h =
            new StringBuilder();

        for (byte b : d) {
            h.append(
                String.format(
                    "%02x",
                    b & 255
                )
            );
        }

        return h.toString();
    }
}
