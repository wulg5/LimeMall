package com.limemall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limemall.common.BusinessException;
import com.limemall.dto.BannerSaveDTO;
import com.limemall.dto.NoticeSaveDTO;
import com.limemall.entity.CmsBanner;
import com.limemall.entity.CmsNotice;
import com.limemall.mapper.CmsBannerMapper;
import com.limemall.mapper.CmsNoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内容运营服务：首页轮播图与公告的维护与读取。
 */
@Service
@RequiredArgsConstructor
public class ContentService {

    private final CmsBannerMapper bannerMapper;
    private final CmsNoticeMapper noticeMapper;

    /** 前台可见轮播 */
    public List<CmsBanner> activeBanners() {
        return bannerMapper.selectList(new LambdaQueryWrapper<CmsBanner>()
                .eq(CmsBanner::getStatus, 1)
                .orderByDesc(CmsBanner::getSort)
                .orderByAsc(CmsBanner::getId));
    }

    /** 前台可见公告 */
    public List<CmsNotice> activeNotices() {
        return noticeMapper.selectList(new LambdaQueryWrapper<CmsNotice>()
                .eq(CmsNotice::getStatus, 1)
                .orderByDesc(CmsNotice::getSort)
                .orderByAsc(CmsNotice::getId));
    }

    /** 后台全部轮播 */
    public List<CmsBanner> allBanners() {
        return bannerMapper.selectList(new LambdaQueryWrapper<CmsBanner>()
                .orderByDesc(CmsBanner::getSort)
                .orderByAsc(CmsBanner::getId));
    }

    /** 后台全部公告 */
    public List<CmsNotice> allNotices() {
        return noticeMapper.selectList(new LambdaQueryWrapper<CmsNotice>()
                .orderByDesc(CmsNotice::getSort)
                .orderByAsc(CmsNotice::getId));
    }

    public void saveBanner(BannerSaveDTO dto) {
        CmsBanner banner = new CmsBanner();
        banner.setId(dto.getId());
        banner.setTag(dto.getTag());
        banner.setTitle(dto.getTitle());
        banner.setSubtitle(dto.getSubtitle());
        banner.setCtaText(dto.getCtaText());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setBgFrom(dto.getBgFrom());
        banner.setBgTo(dto.getBgTo());
        banner.setImage1(dto.getImage1());
        banner.setImage2(dto.getImage2());
        banner.setSort(dto.getSort() == null ? 0 : dto.getSort());
        banner.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (dto.getId() == null) {
            bannerMapper.insert(banner);
        } else {
            bannerMapper.updateById(banner);
        }
    }

    public void deleteBanner(Long id) {
        bannerMapper.deleteById(id);
    }

    public void saveNotice(NoticeSaveDTO dto) {
        CmsNotice notice = new CmsNotice();
        notice.setId(dto.getId());
        notice.setContent(dto.getContent());
        notice.setSort(dto.getSort() == null ? 0 : dto.getSort());
        notice.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (dto.getId() == null) {
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }
    }

    public void deleteNotice(Long id) {
        if (noticeMapper.selectById(id) == null) {
            throw new BusinessException("公告不存在");
        }
        noticeMapper.deleteById(id);
    }
}
