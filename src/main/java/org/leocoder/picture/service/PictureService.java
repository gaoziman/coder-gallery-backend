package org.leocoder.picture.service;

import org.leocoder.picture.domain.dto.picture.PictureEditRequest;
import org.leocoder.picture.domain.dto.picture.PictureUploadByBatchRequest;
import org.leocoder.picture.domain.dto.picture.PictureUploadRequest;
import org.leocoder.picture.domain.dto.picture.PictureWaterfallRequest;
import org.leocoder.picture.domain.pojo.Picture;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.picture.PictureVO;
import org.leocoder.picture.domain.vo.picture.PictureWaterfallVO;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 00:25
 * @description : 图片服务接口
 */
public interface PictureService {


    /**
     * 填充审核参数
     *
     * @param picture   图片信息
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);


    /**
     * 上传图片
     *
     * @param inputSource  来源对象（URL上传或者文件上传）
     * @param requestParam 请求参数
     * @param loginUser    登录用户
     * @return PictureVO
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest requestParam, User loginUser);


    /**
     * 根据ID获取图片详情
     *
     * @param id        图片ID
     * @param loginUser 当前登录用户
     * @return 图片详情
     */
    PictureVO getPictureById(Long id, User loginUser);

    /**
     * 获取上一张图片
     *
     * @param currentPictureId 当前图片ID
     * @param loginUser        当前登录用户
     * @return 上一张图片详情，如果没有则返回null
     */
    PictureVO getPreviousPicture(Long currentPictureId, User loginUser);

    /**
     * 获取下一张图片
     *
     * @param currentPictureId 当前图片ID
     * @param loginUser        当前登录用户
     * @return 下一张图片详情，如果没有则返回null
     */
    PictureVO getNextPicture(Long currentPictureId, User loginUser);


    /**
     * 批量抓取图片
     *
     * @param requestParam 批量抓取参数
     * @param loginUser    登录用户
     * @return 上传成功的图片数量
     */
    int uploadPictureByBatch(PictureUploadByBatchRequest requestParam, User loginUser);


    /**
     * 获取首页瀑布流图片列表
     *
     * @param requestParam   请求参数（排序方式、筛选条件等）
     * @param loginUser 当前登录用户
     * @return 瀑布流图片列表包装对象
     */
    PictureWaterfallVO getWaterfallPictures(PictureWaterfallRequest requestParam, User loginUser);

    /**
     * 加载更多瀑布流图片
     *
     * @param lastId    最后一张图片ID（游标）
     * @param lastValue 最后一张图片的排序值
     * @param requestParam   请求参数（与初始请求一致，确保条件统一）
     * @param loginUser 当前登录用户
     * @return 更多的瀑布流图片列表包装对象
     */
    PictureWaterfallVO loadMoreWaterfallPictures(Long lastId, Object lastValue,
                                                 PictureWaterfallRequest requestParam, User loginUser);


    /**
     * 校验图片信息
     *
     * @param picture 图片信息
     */
    void validPicture(Picture picture);


    /**
     * 图片校验
     *
     * @param loginUser 登录用户
     * @param picture   图片信息
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 编辑图片信息（包括分类和标签）
     *
     * @param requestParam 编辑参数
     * @return 是否成功
     */
    Boolean editPicture(PictureEditRequest requestParam);

}