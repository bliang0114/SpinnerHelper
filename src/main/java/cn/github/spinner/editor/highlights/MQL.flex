package cn.github.spinner.editor.highlights;

import cn.github.spinner.editor.MQLKeywords;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

// 词法分析器配置项
%class _MQLLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{
    return;
%eof}

// 正则表达式宏定义（可复用的语法规则）
WHITE_SPACE    = [\ \n\t\f]                  // 空白字符（空格、换行、制表符、换页符）
DIGIT          = [0-9]                       // 数字
LETTER         = [a-zA-Z_]                   // 字母（含下划线）
IDENTIFIER     = {LETTER}({LETTER}|{DIGIT})* // 标识符（字母开头，后接字母/数字）

LINE_COMMENT   = "//"[^\r\n]*                // 单行注释（// 开头，到行尾结束）
BLOCK_COMMENT  = "/"\*([^*]|\*+[^*/])*(\*+"/")? // 多行注释（/* 开头，*/ 结尾，支持嵌套）

STRING         = \"([^\\\"\r\n]|\\[^\r\n])*\"? // 字符串（双引号包裹，支持转义字符）
CHAR           = '([^\\']|\\[^])*'?           // 字符（单引号包裹，支持转义字符）
NUMBER         = ({DIGIT}+|{DIGIT}*\.{DIGIT}+)([eE][-+]?{DIGIT}+)? // 数值（整数/浮点数/科学计数法）

%%

// YYINITIAL：默认词法状态（初始状态）
<YYINITIAL> {
    // 1. 空白字符处理
    {WHITE_SPACE}          { return com.intellij.psi.TokenType.WHITE_SPACE; }

    // 2. 注释处理
    #[^\n]*                { return MQLTokenTypes.COMMENT; }

    // 3. 字面量处理（字符串、字符、数值）
    {STRING}               { return MQLTokenTypes.STRING; }
    {CHAR}                 { return MQLTokenTypes.STRING; }
    {NUMBER}               { return MQLTokenTypes.NUMBER; }

    {IDENTIFIER} {
        String text = yytext().toString();
        String lowerText = text.toLowerCase();

        if (MQLKeywords.KEYWORDS.contains(lowerText)) { // 4. 关键字（语言保留字）
            return MQLTokenTypes.KEYWORD;
        } else if (MQLKeywords.TYPES.contains(lowerText)) { // 5. 类型关键字（数据类型/实体类型）
            return MQLTokenTypes.TYPE;
        } else if (MQLKeywords.OPERATORS.contains(lowerText)) { // 6. 操作符与分隔符
             return MQLTokenTypes.OPERATOR;
        } else {
            return MQLTokenTypes.IDENTIFIER; // 7. 标识符（变量名、函数名等非保留字）
        }
    }

    // 8. 非法字符（无法识别的）
    [^]                    { return com.intellij.psi.TokenType.BAD_CHARACTER; }
}