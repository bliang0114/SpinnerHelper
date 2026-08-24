package cn.github.spinner.task;

import junit.framework.TestCase;

import java.util.List;

public class MQLCommandParserTest extends TestCase {
    public void testParsesOnlyExecutableMqlOutsideComments() {
        String source = "// ignored;\n"
                + "print bus \"A//B\" * *; /* ignored;\n"
                + "still ignored */ print type Part;\n"
                + "# ignored too\n";

        List<MQLCommandEntry> entries = MQLCommandParser.parse(source, 0, source.length(), ";");

        assertEquals(2, entries.size());
        assertEquals("print bus \"A//B\" * *", entries.get(0).command());
        assertEquals(1, entries.get(0).lineNumber());
        assertEquals("print type Part", entries.get(1).command());
        assertEquals(2, entries.get(1).lineNumber());
    }

    public void testUnderstandsBlockCommentThatStartsBeforeExecutionRange() {
        String source = "/* opening\n"
                + "still ignored */ print type Part;";
        int rangeStart = source.indexOf("still");

        List<MQLCommandEntry> entries = MQLCommandParser.parse(source, rangeStart, source.length(), ";");

        assertEquals(1, entries.size());
        assertEquals("print type Part", entries.get(0).command());
        assertEquals(1, entries.get(0).lineNumber());
    }
}
