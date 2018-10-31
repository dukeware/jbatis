package com.ibatis.common.xml;

public class NodeletException extends RuntimeException {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -9054095133088271232L;

    public NodeletException(String msg) {
        super(msg);
    }

    public NodeletException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
