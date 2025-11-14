import com.matrixone.apps.domain.util.Tenant;
import com.matrixone.apps.framework.ui.UICache;
import matrix.db.Context;
import matrix.db.Person;
import matrix.util.MatrixException;

import java.io.*;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpinnerDeployJPO {

    final boolean adminOnly = true;

    public Boolean fileExists(Context ctx, String[] args) throws Exception {
        if (adminOnly)
            checkAdmin(ctx);
        File file = new File(args[0]);
        return file.exists();
    }

    public Properties getEnv(Context ctx, String[] args) throws Exception {
        if (adminOnly)
            checkAdmin(ctx);
        Properties props = new Properties();
        props.putAll(System.getenv());
        return props;
    }

    public Properties getProperties(Context ctx, String[] args) throws Exception {
        if (adminOnly)
            checkAdmin(ctx);
        return System.getProperties();
    }

    public String getLatestFile(Context ctx, String[] args) throws Exception {
        if (adminOnly)
            checkAdmin(ctx);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date d = new Date();
        for (int i = 0; i < 365; i++) {
            String filename = args[0].replace("#latestdate#", df.format(d));
            File f = new File(filename);
            if (f.exists())
                return filename;
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            cal.add(Calendar.DAY_OF_YEAR, -1);
            d = cal.getTime();
        }
        return null;
    }

    public Object readDir(Context ctx, String[] args) throws Exception {
        if (adminOnly)
            checkAdmin(ctx);
        File dir = new File(args[0]);
        String[] subdirs = dir.list((File current, String name) -> new File(current, name).isDirectory());
        String[] files = dir.list((File current, String name) -> !new File(current, name).isDirectory());
        HashMap result = new HashMap();
        result.put("subdirs", subdirs);
        result.put("files", files);
        return result;
    }

    public String readFile(Context ctx, String[] args) throws Exception {
        if (adminOnly)
            checkAdmin(ctx);
        try (FileInputStream fis = new FileInputStream(args[0])) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0)
                sb.append(buffer, 0, read);
            return sb.toString();
        } catch (IOException e) {
            throw e;
        }
    }

    public void writeFile(Context ctx, String[] args) throws Exception {
        if (adminOnly)
            checkAdmin(ctx);
        try {
            try (PrintWriter pw = new PrintWriter(args[0])) {
                pw.print(args[1]);
                pw.flush();
            }
        } catch (FileNotFoundException e) {
            throw e;
        }
    }

    public String runScript(Context ctx, String[] args) throws Exception {
        if (adminOnly)
            checkAdmin(ctx);
        String[] cmdarray = Arrays.copyOf(args, args.length - 2);
        String dir = args[args.length - 2];
        String output = args[args.length - 1];
        ProcessBuilder pb = new ProcessBuilder(cmdarray);
        pb.directory(new File(dir));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StreamWriter sw = new StreamWriter(p.getInputStream(), output);
        sw.run();
        return readFile(ctx, new String[]{output});
    }

    private void checkAdmin(Context ctx) throws MatrixException {
        String user = ctx.getUser();
        Person p = new Person(user);
        p.open(ctx);
        boolean admin = p.isSystemAdmin();
        p.close(ctx);
        if (!admin)
            throw new MatrixException("Context user \'" + user + "\' is not authorized for system administration");
    }

    public void reCachePage(Context ctx, String[] args) throws Exception {
        if (adminOnly)
            checkAdmin(ctx);
        UICache._tenantPageNames.remove("globalUser");
        UICache._tenantContent.remove("globalUser");
        // 参数：方法名、参数类型数组（与原方法参数类型一一对应）
        Method  buildTenantPropertyCache = Tenant.class.getDeclaredMethod(
                "buildTenantPropertyCache",
                Context.class,
                String.class,
                String.class,
                boolean.class
        );
        buildTenantPropertyCache.setAccessible(true);
        buildTenantPropertyCache.invoke(
                null, // 静态方法无需实例，传 null
                ctx,  // 第一个参数：Context 实例
                "globalUser",  // 第二个参数：String
                "Conf",        // 第三个参数：String
                true           // 第四个参数：Boolean
        );


    }

    class StreamWriter extends Thread {

        private InputStream is;
        private String filename;

        public StreamWriter(InputStream is, String filename) {
            this.is = is;
            this.filename = filename;
        }

        @Override
        public void run() {
            BufferedReader in = null;
            BufferedWriter out = null;
            try {
                in = new BufferedReader(new InputStreamReader(is));
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
                String s;
                while ((s = in.readLine()) != null) {
                    out.write(s);
                    out.write("\\n");
                }
            } catch (IOException ex) {
            } finally {
                try {
                    if (out != null)
                        out.close();
                    if (in != null)
                        in.close();
                } catch (IOException ex) {
                }
            }
        }
    }
}