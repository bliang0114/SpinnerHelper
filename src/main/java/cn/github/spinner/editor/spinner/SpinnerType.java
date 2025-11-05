package cn.github.spinner.editor.spinner;

public enum SpinnerType {
    ATTRIBUTE, INTERFACE, POLICY, POLICY_RULE_MAPPING, POLICY_STATE, PROPERTY, RELATIONSHIP, TRIGGER, TYPE,
    PAGE, PROGRAM, ROLE, RULE, NUMBER_GENERATOR, OBJECT_GENERATOR, TRIGGER_PROGRAM_PARAMETERS, REL_NUMBER_GENERATOR,
    CHANNEL, COMMAND, MENU, PORTAL, TABLE_COLUMN, TABLE, FORM, FORM_FIELD;

    public static SpinnerType fromFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) throw new IllegalArgumentException("Error: fileName is null or empty");

        return switch (fileName) {
            case "SpinnerAttributeData_ALL.xls" -> ATTRIBUTE;
            case "SpinnerChannelData_ALL.xls" -> CHANNEL;
            case "SpinnerCommandData_ALL.xls" -> COMMAND;
            case "SpinnerInterfaceData_ALL.xls" -> INTERFACE;
            case "SpinnerMenuData_ALL.xls" -> MENU;
            case "SpinnerPageData_ALL.xls" -> PAGE;
            case "SpinnerPolicyData_ALL.xls" -> POLICY;
            case "SpinnerPolicyRuleMappingData_ALL.xls" -> POLICY_RULE_MAPPING;
            case "SpinnerPolicyStateData_ALL.xls" -> POLICY_STATE;
            case "SpinnerPortalData_ALL.xls" -> PORTAL;
            case "SpinnerProgramData_ALL.xls" -> PROGRAM;
            case "SpinnerPropertyData_ALL.xls" -> PROPERTY;
            case "SpinnerRelationshipData_ALL.xls" -> RELATIONSHIP;
            case "SpinnerRoleData_ALL.xls" -> ROLE;
            case "SpinnerRuleData_ALL.xls" -> RULE;
            case "SpinnerTableColumnData_ALL.xls" -> TABLE_COLUMN;
            case "SpinnerTableData_ALL.xls" -> TABLE;
            case "SpinnerTriggerData_ALL.xls" -> TRIGGER;
            case "SpinnerTypeData_ALL.xls" -> TYPE;
            case "SpinnerWebFormData_ALL.xls" -> FORM;
            case "SpinnerWebFormFieldData_ALL.xls" -> FORM_FIELD;
            case "bo_eService Number Generator_ALL.xls" -> NUMBER_GENERATOR;
            case "bo_eService Object Generator_ALL.xls" -> OBJECT_GENERATOR;
            case "bo_eService Trigger Program Parameters_ALL.xls" -> TRIGGER_PROGRAM_PARAMETERS;
            case "rel-b2b_eService Number Generator_ALL.xls" -> REL_NUMBER_GENERATOR;
            default -> throw new IllegalArgumentException("Error: Unknown file: " + fileName);
        };
    }
}
