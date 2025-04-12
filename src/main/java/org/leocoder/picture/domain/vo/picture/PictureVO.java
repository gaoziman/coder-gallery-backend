package org.leocoder.picture.domain.vo.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.leocoder.picture.domain.pojo.Picture;
import org.leocoder.picture.domain.vo.user.UserVO;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2024-12-12 23:14
 * @description : 图片对象返回VO
 */
@Data
@ApiModel(value = "PictureVO", description = "图片对象返回VO")
public class PictureVO implements Serializable {

    @ApiModelProperty(value = "id", name = "id")
    private Long id;

    @ApiModelProperty(value = "图片名称", name = "name")
    private String name;


    @ApiModelProperty(value = "图片 url", name = "url")
    private String url;

    @ApiModelProperty(value = "缩略图 url", name = "thumbnailUrl")
    private String thumbnailUrl;

    @ApiModelProperty(value = "原始文件名", name = "originalName")
    private String originalName;


    @ApiModelProperty(value = "描述", name = "introduction")
    private String description;


    @ApiModelProperty(value = "图片格式", name = "picSize")
    private String format;

    @ApiModelProperty(value = "图片宽度", name = "picWidth")
    private Integer picWidth;

    @ApiModelProperty(value = "图片高度", name = "picHeight")
    private Integer picHeight;

    @ApiModelProperty(value = "图片比例", name = "picScale")
    private Double picScale;


    @ApiModelProperty(value = "图片大小", name = "size")
    private Long size;

    @ApiModelProperty(value = "图片主色调", name = "mainColor")
    private String mainColor;

    @ApiModelProperty(value = "标签Ids", name = "tags")
    private List<String> tagIds;

    @ApiModelProperty(value = "分类Id", name = "category")
    private String categoryId;

    @ApiModelProperty(value = "标签名称", name = "tags")
    private List<String> tags;

    @ApiModelProperty(value = "分类名称", name = "category")
    private String category;

    @ApiModelProperty(value = "审核状态", name = "reviewStatus")
    private Integer reviewStatus;

    @ApiModelProperty(value = "审核内容", name = "reviewMessage")
    private String reviewMessage;

    @ApiModelProperty(value = "审核人 id", name = "reviewerId")
    private Long reviewerId;

    @ApiModelProperty(value = "审核时间", name = "reviewTime")
    private LocalDateTime reviewTime;

    @ApiModelProperty(value = "创建时间", name = "createTime")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "图片点赞数", name = "likeCount")
    private Integer likeCount;

    @ApiModelProperty(value = "收藏数量", name = "favoriteCount")
    private Integer collectionCount;

    @ApiModelProperty(value = "浏览量", name = "viewCount")
    private Long viewCount;

    @ApiModelProperty(value = "创建用户信息", name = "user")
    private UserVO user;


    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     *
     * @param pictureVO 图片对象
     * @return Picture
     */
    public static Picture voToObj(PictureVO pictureVO) {
        if (pictureVO == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVO, picture);
        return picture;
    }

    /**
     * 封装对象转类
     *
     * @param picture 图片对象
     * @return PictureVO
     */
    public static PictureVO objToVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(picture, pictureVO);
        return pictureVO;
    }
}
