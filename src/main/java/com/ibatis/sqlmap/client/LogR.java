package com.ibatis.sqlmap.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import com.ibatis.sqlmap.client.lexer.SqlLexer;
import com.ibatis.sqlmap.client.lexer.Token;

public class LogR {

    static class MeIComparator implements Comparator<Map.Entry<String, int[]>> {
        @Override
        public int compare(Entry<String, int[]> o1, Entry<String, int[]> o2) {
            return o2.getValue()[0] - o1.getValue()[0];
        }
    }

    static Map<String, int[]> qmap = new HashMap<String, int[]>();
    static Map<String, int[]> fmap = new HashMap<String, int[]>();
    static Map<String, int[]> omap = new HashMap<String, int[]>();

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("java -jar jbatis-2.4.x.jar <text-file> <gz-file> ...");
            return;
        }
        for (String arg : args) {
            File f = new File(arg);
            if (!f.isFile()) {
                System.err.println("Bad log file: " + args[0]);
                continue;
            }
            statsLog(f);
        }
        showStats();
    }

    static void statsLog(File f) throws FileNotFoundException, IOException {
        InputStream is = new FileInputStream(f);
        Reader r = null;
        if (f.getName().endsWith(".gz")) {
            r = new InputStreamReader(new GZIPInputStream(is));
        } else {
            r = new InputStreamReader(is);
        }
        BufferedReader br = new BufferedReader(r);

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            int idx = line.indexOf("-- Current_sql - ");
            if (idx >= 0) {
                String sql = line.substring(idx + 17);
                SqlLexer sl = new SqlLexer(sql);
                Token head = sl.firstKeyword();
                if (head != null) {
                    switch (head.type) {
                    case Select:
                        addQuery(sl.getRoots());
                        break;
                    case Insert:
                    case Replace:
                    case Update:
                    case Delete:
                        addFlush(sl.getRoots());
                        break;
                    default:
                        System.out.println("Unkown sql: " + sql);
                        break;
                    }
                }
            } else {
                idx = line.indexOf("-- Current_obj - ");
                if (idx >= 0) {
                    int of = line.indexOf(" of ", idx + 17);
                    if (of > 0) {
                        addSqlId(line.substring(idx + 17, of));
                    }
                }
            }
        }
        br.close();
    }

    @SuppressWarnings("unchecked")
    static void showStats() {
        System.out.println("---------- object stats ------------");
        Map.Entry<String, int[]>[] me = omap.entrySet().toArray(new Map.Entry[omap.size()]);
        Arrays.sort(me, new MeIComparator());
        for (Map.Entry<String, int[]> e : me) {
            System.out.println(e.getValue()[0] + "\t" + e.getKey());
        }
        System.out.println("---------- query stats ------------");
        me = qmap.entrySet().toArray(new Map.Entry[qmap.size()]);
        Arrays.sort(me, new MeIComparator());
        for (Map.Entry<String, int[]> e : me) {
            System.out.println(e.getValue()[0] + "\t" + e.getKey());
        }
        System.out.println("---------- flush stats ------------");
        me = fmap.entrySet().toArray(new Map.Entry[fmap.size()]);
        Arrays.sort(me, new MeIComparator());
        for (Map.Entry<String, int[]> e : me) {
            System.out.println(e.getValue()[0] + "\t" + e.getKey());
        }
    }

    static void addFlush(List<String> roots) {
        if (roots != null) {
            for (String t : roots) {
                t = t.replace("`", "").toLowerCase();
                int[] c = fmap.get(t);
                if (c == null) {
                    fmap.put(t, new int[] { 1 });
                } else {
                    c[0]++;
                }
            }
        }
    }

    static void addQuery(List<String> roots) {
        if (roots != null) {
            for (String t : roots) {
                t = t.replace("`", "").toLowerCase();
                int[] c = qmap.get(t);
                if (c == null) {
                    qmap.put(t, new int[] { 1 });
                } else {
                    c[0]++;
                }
            }
        }
    }

    static void addSqlId(String id) {
        int[] c = omap.get(id);
        if (c == null) {
            omap.put(id, new int[] { 1 });
        } else {
            c[0]++;
        }
    }

}
