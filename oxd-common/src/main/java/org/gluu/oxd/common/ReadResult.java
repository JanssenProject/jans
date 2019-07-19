/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.common;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 28/07/2013
 */
public class ReadResult {

    private String m_command;
    private String m_leftString;

    public ReadResult() {
    }

    public ReadResult(String command, String leftString) {
        m_command = command;
        m_leftString = leftString;
    }

    public String getCommand() {
        return m_command;
    }

    public void setCommand(String command) {
        m_command = command;
    }

    public String getLeftString() {
        return m_leftString;
    }

    public void setLeftString(String leftString) {
        m_leftString = leftString;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ReadResult");
        sb.append("{m_command='").append(m_command).append('\'');
        sb.append(", m_leftString='").append(m_leftString).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
