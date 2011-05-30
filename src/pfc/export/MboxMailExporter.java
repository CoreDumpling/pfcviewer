/*
 * Copyright (c) 2002 Ernest Yu. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to 
 * deal in the Software without restriction, including without limitation the 
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
 * sell copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package pfc.export;

import java.io.*;
import java.text.*;
import java.util.Date;
import pfc.cab.*;

/**
 *  Class to export mail messages to an mbox mail file.  This is a flat 
 *  file export; the directory tree structure is not preserved in the 
 *  file.
 *
 *  @author Ernie Yu
 *  <br>15 Mar 2003 Opens DataOutputStream to count bytes written.
 */
public class MboxMailExporter implements Exporter {

    protected MboxFile mbox;
    protected int msgCount;

    /** Creates a new instance of MboxMailExporter */
    public MboxMailExporter() {
        mbox = null;
        msgCount = 0;
    }
    
    /** Sets file to receive exported items.
     */
    public void setFile(File exportFile) {
        try {
            mbox = new MboxFile(exportFile);
        } catch (FileNotFoundException ex) {
            System.err.println("Unable to set mbox file: " + exportFile);
        }
    }
    
    /** Returns true if cabinet item is valid for export.  For mbox mail
     *  exports, the item must be a mail envelope.
     */
    public boolean isExportable(CabinetItem item) {
        if ((item.getType() == CabinetItem.MAIL_ENVELOPE) &&
            (item.getData() != 0)) {
            return true;
        } else {
            return false;
        }
    }

    /** Opens export file.
     */
    public void open() throws IOException {
    }
    
    /** Opens folder in export file.  Does nothing here.
     */
    public void openFolder(CabinetItem item) {
    }

    /** Exports cabinet item to export file.  All mail information is
     *  taken from the mail item; the envelope item is ignored.
     */
    public void export(CabinetItem envelope, CabinetItem item) 
        throws IOException {
        PrintWriter out = mbox.getPrintWriter();
        DataOutputStream dos = mbox.getDataOutputStream();
        // Create mail message by parsing item contents.
        MailMessage message = new MailMessage(item.getContent());
        Date date = message.getDate();
        String attachment = message.getAttachment();
        // Write From line with date to file.
        if (date != null) {
            out.println("From - " + toAsctime(date));
        } else {
            out.println("From - " + message.getDateString());
        }
        // Write header, and blank line before body.
        out.println(message.getMailHeader());
        out.println();
        // Insert attachment name if available.
        if (attachment != null) {
            out.println("[" + attachment + "]");
        }
        // Write body, and blank line at end of message.
        out.flush();
        byte[] bodyEscaped = indentFromInBody(message.getBody());
        dos.write(bodyEscaped, 0, bodyEscaped.length);
        dos.flush();
        out.println();
        out.flush();
        // Increment message count.
        msgCount++;
    }

    /** Closes folder in export file.  Does nothing here.
     */
    public void closeFolder() {
    }

    /** Closes export file.
     */
    public void close() {
        mbox.close();
    }

    /** Converts date to asctime date format.  The result string is of the
     *  form: Sun Dec 08 13:59:59 2002.
     */
    public static String toAsctime(Date date) {
        // Create formatter with output date pattern.
        SimpleDateFormat format = new SimpleDateFormat(
            "EEE MMM dd HH:mm:ss yyyy");
        return format.format(date);
    }

    /** Indents lines in the message body that begin with 'From '.  These
     *  lines are quoted by inserting a &gt; character at the start.  This
     *  distinguishes the lines from the first line of an mbox mail message.
     */
    private byte[] indentFromInBody(byte[] body) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String separator = System.getProperty("line.separator");
        byte fromAtStart[] = (separator + "From ").getBytes();
        byte fromEscaped[] = (separator + ">From ").getBytes();
        int b = 0;
        int f = 0;
        // First, check the degenerate case - message body starts with "From "
        if (body.length >= 5 && body[0] == 'F' && body[1] == 'r' &&
            body[2] == 'o' && body[3] == 'm') {
            byte leadingFrom[] = {'>', 'F', 'r', 'o', 'm', ' '};
            out.write(leadingFrom, 0, leadingFrom.length);
            b = 5;
        }
        // Perform a special case of the Knuth-Morris-Pratt search, where only
        // the first character needs to be checked for a new match whenever a
        // mismatch is found
        while (b < body.length - fromAtStart.length) {
            if (body[b + f] == fromAtStart[f]) {
                // we have a matching character
                if (f == fromAtStart.length - 1) {
                    // we have a full match
                    out.write(fromEscaped, 0, fromEscaped.length);
                    b += f + 1;
                    f = 0;
                } else {
                    // continue checking the next character
                    ++f;
                }
            } else {
                // we have a mismatch ...
                if (body[b + f] == fromAtStart[0]) {
                    // ... that is the beginning of a new match
                    out.write(body, b, f);
                    b += f;
                    f = 1;
                } else {
                    // ... that does not start a new match
                    out.write(body, b, 1);
                    ++b;
                    f = 0;
                }
            }
        }
        out.write(body, b, body.length - b);
        return out.toByteArray();
    }
    
}
