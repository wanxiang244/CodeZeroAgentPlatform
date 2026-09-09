package com.ljj.CodeZeroAgentPlatform.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.ljj.CodeZeroAgentPlatform.model.dto.AdminAppUpdateRequest;
import com.ljj.CodeZeroAgentPlatform.model.dto.AppQueryRequest;
import com.ljj.CodeZeroAgentPlatform.model.entity.App;
import com.ljj.CodeZeroAgentPlatform.model.vo.AppVO;
import reactor.core.publisher.Flux;

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
     * 分页查询用户自己的应用
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param userId   用户 id
     * @return 分页结果
     */
    Page<AppVO> listMyAppByPage(long pageNum, long pageSize, Long userId);

    /**
     * 分页查询精选应用（包括自己的）
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param userId   当前用户 id（可为空）
     * @return 分页结果
     */
    Page<AppVO> listFeaturedAppByPage(long pageNum, long pageSize, Long userId);

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
    boolean adminUpdateApp(AdminAppUpdateRequest adminAppUpdateRequest);

    /**
     * 对话生成代码（流式）
     * 根据应用 id 获取应用信息，使用初始提示词生成代码
     *
     * @param appId 应用 id
     * @param userId 用户 id
     * @return 流式代码生成响应
     */
    Flux<String> chatToGenCode(Long appId, Long userId, String userMessage);

    /**
     * 检查deployKey是否存在
     *
     * @param deployKey 部署标识
     * @return 是否存在
     */
    boolean existsByDeployKey(String deployKey);


}
