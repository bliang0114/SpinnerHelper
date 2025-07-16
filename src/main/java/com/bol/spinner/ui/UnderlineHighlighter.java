package com.bol.spinner.ui;

import javax.swing.text.*;
import java.awt.*;

class UnderlineHighlighter extends DefaultHighlighter {
    protected static final HighlightPainter sharedPainter = new UnderlineHighlightPainter((Color)null);
    protected HighlightPainter painter;

    public UnderlineHighlighter(Color c) {
        this.painter = (HighlightPainter)(c == null ? sharedPainter : new UnderlineHighlightPainter(c));
    }

    public Object addHighlight(int p0, int p1) throws BadLocationException {
        return this.addHighlight(p0, p1, this.painter);
    }

    public void setDrawsLayeredHighlights(boolean newValue) {
        if (!newValue) {
            throw new IllegalArgumentException("UnderlineHighlighter only draws layered highlights");
        } else {
            super.setDrawsLayeredHighlights(true);
        }
    }

    public static class UnderlineHighlightPainter extends LayerPainter {
        protected Color color;

        public UnderlineHighlightPainter(Color c) {
            this.color = c;
        }

        public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
        }

        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
            g.setColor(this.color == null ? c.getSelectionColor() : this.color);
            Rectangle alloc;
            if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) {
                if (bounds instanceof Rectangle) {
                    alloc = (Rectangle)bounds;
                } else {
                    alloc = bounds.getBounds();
                }
            } else {
                try {
                    Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
                    alloc = shape instanceof Rectangle ? (Rectangle)shape : shape.getBounds();
                } catch (BadLocationException var10) {
                    return null;
                }
            }

            FontMetrics fm = c.getFontMetrics(c.getFont());
            int baseline = alloc.y + alloc.height - fm.getDescent() + 1;
            g.drawLine(alloc.x, baseline, alloc.x + alloc.width, baseline);
            g.drawLine(alloc.x, baseline + 1, alloc.x + alloc.width, baseline + 1);
            return alloc;
        }
    }
}
