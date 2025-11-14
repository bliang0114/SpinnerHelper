package cn.github.spinner.util;

import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author fzhang
 * @date 2025/11/13
 */
@Slf4j
public class DeployUtil {

    public static void installDeployJpo(Project project) {
        installJpo(project, "SpinnerDeployJPO");
    }

    private static void installJpo(Project project,String fileName) {
        StringBuilder sb = new StringBuilder();
        try (
                InputStream fileInputStream = DeployUtil.class.getResourceAsStream("/jpo/" + fileName + ".java");

                BufferedReader in = new BufferedReader(new InputStreamReader(fileInputStream));
        ) {
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                } else {
                    sb.append(line).append('\n');
                }
            }
            String code = sb.toString();
            code = code.replace(fileName, "${CLASSNAME}");
            String var10000 = code.replace("\\", "\\\\");
            code = "'" + var10000.replace("'", "\\'") + "'";
            String result = MQLUtil.execute(project, "list program " + fileName);
            if (result.isEmpty()) {
                MQLUtil.execute(project, "escape add program " + fileName + " java code " + code);
            }
            // else {
            //     // MQLUtil.execute(project, "escape mod program EnoBrowserJPO code " + code2);
            // }
        } catch (Exception ex2) {
            UIUtil.showErrorNotification(project, "Install " + fileName, ex2.getLocalizedMessage());
        }
    }



}
