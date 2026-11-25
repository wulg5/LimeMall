package com.limemall.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员列表项视图（脱敏，不含密码）。
 */
@Data
public class MemberVO {

    private Long id;

    /** 会员业务编号（对外展示用，不可遍历） */
    private String userNo;

    private String username;

    private String nickname;

    private String phone;

    private String avatar;

    private Integer gender;

    private LocalDate birthday;

    private Integer level;

    private Integer points;

    private Integer status;

    private LocalDateTime registerTime;
}
