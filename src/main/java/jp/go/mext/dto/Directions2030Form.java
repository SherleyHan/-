package jp.go.mext.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知邮件管理画面 Form：用于前后端表单绑定
 */
@Data
public class Directions2030Form {
    /** 调查类别（路径变量）：SY / HI / HL ... */
    private String gamenSyubetu;
    /** 画面 ID */
    private String gamenId;
    /** 画面名称 */
    private String gamenName;
    /** 当前邮件地址列表 */
    private List<String> mailList = new ArrayList<>();
    /** 上传的文件（reflect 时使用） */
    private MultipartFile uploadFile;
    /** 原始文件名（用于校验） */
    private String originalFileName;
    /** 文件扩展名 */
    private String fileExt;
    /** 编辑后的邮件地址字符串（多个用半角冒号分隔） */
    private String mailAddressEdit;
}
