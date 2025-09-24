package cn.github.driver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 3DExperience Matrix 驱动属性
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
