package com.vivi.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class UserDTO implements Serializable {
    private Long id;
    private String nickName;
    private String icon;
}
