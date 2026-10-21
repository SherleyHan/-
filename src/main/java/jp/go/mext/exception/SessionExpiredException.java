package jp.go.mext.exception;

/** 会话失效异常 */
public class SessionExpiredException extends RuntimeException {
    public SessionExpiredException() { super("会话失效"); }
    public SessionExpiredException(String msg) { super(msg); }
}
