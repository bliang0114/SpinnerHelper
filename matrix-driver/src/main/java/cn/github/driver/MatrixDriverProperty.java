package cn.github.driver;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 3DExperience Matrix 驱动属性
 */
@Data
@NoArgsConstructor
public class MatrixDriverProperty {
    /**
     * URL E.g. https://test.mydomain.com/3dspace
     */
    private String url;
    /**
     * User E.g. admin_platform
     */
    private String username;
    /**
     * User Password
     */
    private String password;
    /**
     * Vault E.g. eService Production
     */
    private String vault;
    /**
     * Security Context E.g. ctx::VPLMAdmin.Company Name.Common Space
     */
    private String role;
    /**
     * Whether to login through CAS / 3DPassport first.
     */
    private boolean cas;

    public MatrixDriverProperty(String url, String username, String password, String vault, String role) {
        this(url, username, password, vault, role, false);
    }

    public MatrixDriverProperty(String url, String username, String password, String vault, String role, boolean cas) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.vault = vault;
        this.role = role;
        this.cas = cas;
    }
}
