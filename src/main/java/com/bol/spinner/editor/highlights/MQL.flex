package com.bol.spinner.editor.highlights;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.bol.spinner.editor.highlights.MQLTokenTypes;

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

    // 4. 关键字（语言保留字）
    "add"                  { return MQLTokenTypes.KEYWORD; }
    "abort"                { return MQLTokenTypes.KEYWORD; }
    "assign"               { return MQLTokenTypes.KEYWORD; }
    "check"                { return MQLTokenTypes.KEYWORD; }
    "checkin"              { return MQLTokenTypes.KEYWORD; }
    "checkout"             { return MQLTokenTypes.KEYWORD; }
    "clear"                { return MQLTokenTypes.KEYWORD; }
    "commit"               { return MQLTokenTypes.KEYWORD; }
    "compare"              { return MQLTokenTypes.KEYWORD; }
    "compile"              { return MQLTokenTypes.KEYWORD; }
    "connect"              { return MQLTokenTypes.KEYWORD; }
    "convert"              { return MQLTokenTypes.KEYWORD; }
    "copy"                 { return MQLTokenTypes.KEYWORD; }
    "del"                  { return MQLTokenTypes.KEYWORD; }
    "delete"               { return MQLTokenTypes.KEYWORD; }
    "deduplicatefile"      { return MQLTokenTypes.KEYWORD; }
    "disable"              { return MQLTokenTypes.KEYWORD; }
    "disconnect"           { return MQLTokenTypes.KEYWORD; }
    "download"             { return MQLTokenTypes.KEYWORD; }
    "dump"                 { return MQLTokenTypes.KEYWORD; }
    "duplicatefile"        { return MQLTokenTypes.KEYWORD; }
    "else"                 { return MQLTokenTypes.KEYWORD; }
    "emptyprefix"          { return MQLTokenTypes.KEYWORD; }
    "enable"               { return MQLTokenTypes.KEYWORD; }
    "encrypt"              { return MQLTokenTypes.KEYWORD; }
    "escape"               { return MQLTokenTypes.KEYWORD; }
    "eval"                 { return MQLTokenTypes.KEYWORD; }
    "evaluate"             { return MQLTokenTypes.KEYWORD; }
    "execute"              { return MQLTokenTypes.KEYWORD; }
    "expand"               { return MQLTokenTypes.KEYWORD; }
    "export"               { return MQLTokenTypes.KEYWORD; }
    "extract"              { return MQLTokenTypes.KEYWORD; }
    "freeze"               { return MQLTokenTypes.KEYWORD; }
    "from"                 { return MQLTokenTypes.KEYWORD; }
    "get"                  { return MQLTokenTypes.KEYWORD; }
    "global"               { return MQLTokenTypes.KEYWORD; }
    "help"                 { return MQLTokenTypes.KEYWORD; }
    "hidden"               { return MQLTokenTypes.KEYWORD; }
    "if"                   { return MQLTokenTypes.KEYWORD; }
    "import"               { return MQLTokenTypes.KEYWORD; }
    "insert"               { return MQLTokenTypes.KEYWORD; }
    "inventory"            { return MQLTokenTypes.KEYWORD; }
    "kill"                 { return MQLTokenTypes.KEYWORD; }
    "link"                 { return MQLTokenTypes.KEYWORD; }
    "list"                 { return MQLTokenTypes.KEYWORD; }
    "listnames"            { return MQLTokenTypes.KEYWORD; }
    "lock"                 { return MQLTokenTypes.KEYWORD; }
    "log"                  { return MQLTokenTypes.KEYWORD; }
    "major"                { return MQLTokenTypes.KEYWORD; }
    "matchlist"            { return MQLTokenTypes.KEYWORD; }
    "minor"                { return MQLTokenTypes.KEYWORD; }
    "mod"                  { return MQLTokenTypes.KEYWORD; }
    "modify"               { return MQLTokenTypes.KEYWORD; }
    "monitor"              { return MQLTokenTypes.KEYWORD; }
    "orderby"              { return MQLTokenTypes.KEYWORD; }
    "output"               { return MQLTokenTypes.KEYWORD; }
    "pop"                  { return MQLTokenTypes.KEYWORD; }
    "print"                { return MQLTokenTypes.KEYWORD; }
    "purge"                { return MQLTokenTypes.KEYWORD; }
    "push"                 { return MQLTokenTypes.KEYWORD; }
    "query"                { return MQLTokenTypes.KEYWORD; }
    "rechecksum"           { return MQLTokenTypes.KEYWORD; }
    "recordsep"            { return MQLTokenTypes.KEYWORD; }
    "recordseparator"      { return MQLTokenTypes.KEYWORD; }
    "recurse"              { return MQLTokenTypes.KEYWORD; }
    "rehash"               { return MQLTokenTypes.KEYWORD; }
    "remove"               { return MQLTokenTypes.KEYWORD; }
    "resume"               { return MQLTokenTypes.KEYWORD; }
    "revise"               { return MQLTokenTypes.KEYWORD; }
    "select"               { return MQLTokenTypes.KEYWORD; }
    "send"                 { return MQLTokenTypes.KEYWORD; }
    "set"                  { return MQLTokenTypes.KEYWORD; }
    "smatchlist"           { return MQLTokenTypes.KEYWORD; }
    "sort"                 { return MQLTokenTypes.KEYWORD; }
    "start"                { return MQLTokenTypes.KEYWORD; }
    "status"               { return MQLTokenTypes.KEYWORD; }
    "stop"                 { return MQLTokenTypes.KEYWORD; }
    "substring"            { return MQLTokenTypes.KEYWORD; }
    "sync"                 { return MQLTokenTypes.KEYWORD; }
    "synchronize"          { return MQLTokenTypes.KEYWORD; }
    "tcl"                  { return MQLTokenTypes.KEYWORD; }
    "temp"                 { return MQLTokenTypes.KEYWORD; }
    "temporary"            { return MQLTokenTypes.KEYWORD; }
    "thaw"                 { return MQLTokenTypes.KEYWORD; }
    "then"                 { return MQLTokenTypes.KEYWORD; }
    "tidy"                 { return MQLTokenTypes.KEYWORD; }
    "to"                   { return MQLTokenTypes.KEYWORD; }
    "trace"                { return MQLTokenTypes.KEYWORD; }
    "transition"           { return MQLTokenTypes.KEYWORD; }
    "trigger"              { return MQLTokenTypes.KEYWORD; }
    "unlock"               { return MQLTokenTypes.KEYWORD; }
    "unset"                { return MQLTokenTypes.KEYWORD; }
    "update"               { return MQLTokenTypes.KEYWORD; }
    "updatestate"          { return MQLTokenTypes.KEYWORD; }
    "upload"               { return MQLTokenTypes.KEYWORD; }
    "val"                  { return MQLTokenTypes.KEYWORD; }
    "validate"             { return MQLTokenTypes.KEYWORD; }
    "visible"              { return MQLTokenTypes.KEYWORD; }
    "where"                { return MQLTokenTypes.KEYWORD; }
    "zip"                  { return MQLTokenTypes.KEYWORD; }

    // 5. 类型关键字（数据类型/实体类型）
    "admin"                { return MQLTokenTypes.TYPE; }
    "application"          { return MQLTokenTypes.TYPE; }
    "association"          { return MQLTokenTypes.TYPE; }
    "attribute"            { return MQLTokenTypes.TYPE; }
    "bus"                  { return MQLTokenTypes.TYPE; }
    "businessobject"       { return MQLTokenTypes.TYPE; }
    "businessobjectlist"   { return MQLTokenTypes.TYPE; }
    "channel"              { return MQLTokenTypes.TYPE; }
    "checkshowaccess"      { return MQLTokenTypes.TYPE; }
    "command"              { return MQLTokenTypes.TYPE; }
    "config"               { return MQLTokenTypes.TYPE; }
    "connection"           { return MQLTokenTypes.TYPE; }
    "context"              { return MQLTokenTypes.TYPE; }
    "dataobject"           { return MQLTokenTypes.TYPE; }
    "dimension"            { return MQLTokenTypes.TYPE; }
    "env"                  { return MQLTokenTypes.TYPE; }
    "eventmonitor"         { return MQLTokenTypes.TYPE; }
    "expr"                 { return MQLTokenTypes.TYPE; }
    "expression"           { return MQLTokenTypes.TYPE; }
    "filter"               { return MQLTokenTypes.TYPE; }
    "form"                 { return MQLTokenTypes.TYPE; }
    "format"               { return MQLTokenTypes.TYPE; }
    "group"                { return MQLTokenTypes.TYPE; }
    "history"              { return MQLTokenTypes.TYPE; }
    "index"                { return MQLTokenTypes.TYPE; }
    "inheritancerule"      { return MQLTokenTypes.TYPE; }
    "inquiry"              { return MQLTokenTypes.TYPE; }
    "interface"            { return MQLTokenTypes.TYPE; }
    "location"             { return MQLTokenTypes.TYPE; }
    "mail"                 { return MQLTokenTypes.TYPE; }
    "memory"               { return MQLTokenTypes.TYPE; }
    "menu"                 { return MQLTokenTypes.TYPE; }
    "package"              { return MQLTokenTypes.TYPE; }
    "page"                 { return MQLTokenTypes.TYPE; }
    "password"             { return MQLTokenTypes.TYPE; }
    "path"                 { return MQLTokenTypes.TYPE; }
    "pathtype"             { return MQLTokenTypes.TYPE; }
    "person"               { return MQLTokenTypes.TYPE; }
    "policy"               { return MQLTokenTypes.TYPE; }
    "portal"               { return MQLTokenTypes.TYPE; }
    "product"              { return MQLTokenTypes.TYPE; }
    "program"              { return MQLTokenTypes.TYPE; }
    "property"             { return MQLTokenTypes.TYPE; }
    "rel"                  { return MQLTokenTypes.TYPE; }
    "relationship"         { return MQLTokenTypes.TYPE; }
    "resource"             { return MQLTokenTypes.TYPE; }
    "role"                 { return MQLTokenTypes.TYPE; }
    "rule"                 { return MQLTokenTypes.TYPE; }
    "searchindex"          { return MQLTokenTypes.TYPE; }
    "server"               { return MQLTokenTypes.TYPE; }
    "site"                 { return MQLTokenTypes.TYPE; }
    "store"                { return MQLTokenTypes.TYPE; }
    "system"               { return MQLTokenTypes.TYPE; }
    "table"                { return MQLTokenTypes.TYPE; }
    "tenant"               { return MQLTokenTypes.TYPE; }
    "thread"               { return MQLTokenTypes.TYPE; }
    "toolset"              { return MQLTokenTypes.TYPE; }
    "transaction"          { return MQLTokenTypes.TYPE; }
    "type"                 { return MQLTokenTypes.TYPE; }
    "uniquekey"            { return MQLTokenTypes.TYPE; }
    "user"                 { return MQLTokenTypes.TYPE; }
    "vault"                { return MQLTokenTypes.TYPE; }
    "webreport"            { return MQLTokenTypes.TYPE; }

    // 6. 操作符与分隔符
    // 括号类
    "("                    { return MQLTokenTypes.OPERATOR; }
    ")"                    { return MQLTokenTypes.OPERATOR; }
    "["                    { return MQLTokenTypes.OPERATOR; }
    "]"                    { return MQLTokenTypes.OPERATOR; }

    // 分隔符类
    ";"                    { return MQLTokenTypes.OPERATOR; }
    ","                    { return MQLTokenTypes.OPERATOR; }
    "."                    { return MQLTokenTypes.OPERATOR; }
    "|"                    { return MQLTokenTypes.OPERATOR; }

    // 比较操作符
    "=="                   { return MQLTokenTypes.OPERATOR; }
    "!="                   { return MQLTokenTypes.OPERATOR; }
    "<"                    { return MQLTokenTypes.OPERATOR; }
    ">"                    { return MQLTokenTypes.OPERATOR; }
    "<="                   { return MQLTokenTypes.OPERATOR; }
    ">="                   { return MQLTokenTypes.OPERATOR; }
    // 匹配操作符
    "~~"                   { return MQLTokenTypes.OPERATOR; }
    "!~~"                  { return MQLTokenTypes.OPERATOR; }
    "~="                   { return MQLTokenTypes.OPERATOR; }
    "!~="                  { return MQLTokenTypes.OPERATOR; }

    // 算术操作符
    "+"                    { return MQLTokenTypes.OPERATOR; }
    "-"                    { return MQLTokenTypes.OPERATOR; }
    "*"                    { return MQLTokenTypes.OPERATOR; }
    "/"                    { return MQLTokenTypes.OPERATOR; }

    // 逻辑操作符
    "!"                    { return MQLTokenTypes.OPERATOR; }
    "&&"                   { return MQLTokenTypes.OPERATOR; }
    "||"                   { return MQLTokenTypes.OPERATOR; }

    // 7. 标识符（变量名、函数名等非保留字）
    {IDENTIFIER}           { return MQLTokenTypes.IDENTIFIER; }

    // 8. 非法字符（无法识别的）
    [^]                    { return com.intellij.psi.TokenType.BAD_CHARACTER; }
}