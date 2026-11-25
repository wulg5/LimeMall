package com.limemall.security;

import com.limemall.entity.UmsAdmin;
import com.limemall.entity.UmsMember;
import com.limemall.entity.UmsMenu;
import com.limemall.mapper.UmsAdminMapper;
import com.limemall.mapper.UmsMemberMapper;
import com.limemall.mapper.UmsMenuMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器。
 * 从请求头 Authorization: Bearer <token> 中解析令牌：
 * 1. 管理员令牌：校验账号状态后，加载其角色权限标识（ums_menu.perm）注入安全上下文；
 * 2. 会员令牌：校验会员状态后，注入 ROLE_MEMBER 角色。
 * 解析失败时不中断请求，后续由入口点对受保护接口返回 401。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UmsAdminMapper adminMapper;
    private final UmsMemberMapper memberMapper;
    private final UmsMenuMapper menuMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            try {
                Claims claims = jwtUtil.parseToken(header.substring(PREFIX.length()));
                Long userId = Long.valueOf(claims.getSubject());
                String type = claims.get("type", String.class);
                String username = claims.get("username", String.class);

                List<GrantedAuthority> authorities = new ArrayList<>();
                if (LoginUser.TYPE_ADMIN.equals(type)) {
                    UmsAdmin admin = adminMapper.selectById(userId);
                    if (admin != null && admin.getStatus() == 1) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        List<UmsMenu> menus = menuMapper.selectByAdminId(userId);
                        menus.stream()
                                .map(UmsMenu::getPerm)
                                .filter(perm -> StringUtils.hasText(perm))
                                .distinct()
                                .forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));
                        authenticate(new LoginUser(userId, username, LoginUser.TYPE_ADMIN), authorities);
                    }
                } else if (LoginUser.TYPE_MEMBER.equals(type)) {
                    UmsMember member = memberMapper.selectById(userId);
                    if (member != null && member.getStatus() == 1) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
                        authenticate(new LoginUser(userId, username, LoginUser.TYPE_MEMBER), authorities);
                    }
                }
            } catch (Exception ignored) {
                // 令牌非法或过期：不设置认证信息，由各保护接口统一返回 401
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(LoginUser loginUser, List<GrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
