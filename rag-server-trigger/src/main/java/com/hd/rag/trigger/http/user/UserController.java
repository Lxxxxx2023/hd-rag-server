package com.hd.rag.trigger.http.user;

import cn.dev33.satoken.stp.StpUtil;
import com.hd.rag.api.user.dto.request.UserLoginReqDTO;
import com.hd.rag.api.user.dto.request.UserRegisterReqDTO;
import com.hd.rag.api.user.dto.response.UserInfoRespDTO;
import com.hd.rag.api.user.dto.response.UserLoginRespDTO;
import com.hd.rag.domain.user.model.entity.UserEntity;
import com.hd.rag.domain.user.service.IUserService;
import com.hd.rag.types.convention.Result;
import com.hd.rag.types.exception.ClientException;
import com.hd.rag.types.web.Results;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @PostMapping("/login")
    public Result<UserLoginRespDTO> login(@RequestBody @Valid UserLoginReqDTO reqDTO) {
        UserEntity user = userService.login(reqDTO.username(), reqDTO.password());
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        return Results.success(new UserLoginRespDTO(token, user.getId(), user.getUsername()));
    }

    @PostMapping("/register")
    public Result<UserLoginRespDTO> register(@RequestBody @Valid UserRegisterReqDTO reqDTO) {
        UserEntity user = userService.register(reqDTO.username(), reqDTO.password());
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        return Results.success(new UserLoginRespDTO(token, user.getId(), user.getUsername()));
    }

    @GetMapping("/me")
    public Result<UserInfoRespDTO> currentUser() {
        if (!StpUtil.isLogin()) {
            throw new ClientException("未登录");
        }
        String userId = StpUtil.getLoginIdAsString();
        UserEntity user = userService.getById(userId);
        return Results.success(new UserInfoRespDTO(
                user.getId(), user.getUsername(),
                user.getCreateTime(), user.getUpdateTime()));
    }
}
