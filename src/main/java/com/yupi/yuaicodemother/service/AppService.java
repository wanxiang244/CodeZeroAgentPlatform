package com.yupi.yuaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yupi.yuaicodemother.model.dto.AppQueryRequest;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.model.vo.AppVO;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
public interface AppService extends IService<App> {

    /**
     * 获取脱敏后的应用信息
     *
     * @param app 应用信息
     * @return 脱敏后的应用信息
     */
    AppVO getAppVO(App app);

    /**
     * 获取脱敏后的应用信息（分页）
     *
     * @param appList 应用列表
     * @return 脱敏后的应用信息列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据查询条件构造数据查询参数
     *
     * @param appQueryRequest 查询请求
     * @return 查询包装器
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 删除应用（包含权限校验）
     *
     * @param id 应用 id
     * @param userId 用户 id
     * @param userRole 用户角色
     * @return 是否删除成功
     */
    boolean deleteApp(Long id, Long userId, String userRole);

    /**
     * 管理员删除应用
     *
     * @param id 应用 id
     * @return 是否删除成功
     */
    boolean adminDeleteApp(Long id);

    /**
     * 管理员更新应用
     *
     * @param adminAppUpdateRequest 管理员应用更新请求
     * @return 是否更新成功
     */
    boolean adminUpdateApp(com.yupi.yuaicodemother.model.dto.AdminAppUpdateRequest adminAppUpdateRequest);
}