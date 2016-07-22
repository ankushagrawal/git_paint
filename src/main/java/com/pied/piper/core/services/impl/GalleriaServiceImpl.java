package com.pied.piper.core.services.impl;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.pied.piper.core.db.dao.impl.ImageDaoImpl;
import com.pied.piper.core.db.model.Image;
import com.pied.piper.core.db.model.ImageRelation;
import com.pied.piper.core.db.model.ImageTags;
import com.pied.piper.core.db.model.User;
import com.pied.piper.core.db.model.enums.ApprovalStatusEnum;
import com.pied.piper.core.dto.*;
import com.pied.piper.core.services.interfaces.GalleriaService;
import com.pied.piper.core.services.interfaces.ImageRelationService;
import com.pied.piper.core.services.interfaces.UserService;
import com.pied.piper.exception.ResponseException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by akshay.kesarwan on 21/05/16.
 */
@Slf4j
public class GalleriaServiceImpl implements GalleriaService {

    private final ImageDaoImpl imageDao;
    private final ImageTagServiceImpl imageTagService;
    private final UserService userService;
    private final ImageRelationService imageRelationService;

    @Inject
    public GalleriaServiceImpl(ImageDaoImpl imageDao, ImageTagServiceImpl imageTagService, UserService userService, ImageRelationService imageRelationService) {
        this.imageDao = imageDao;
        this.imageTagService = imageTagService;
        this.userService = userService;
        this.imageRelationService = imageRelationService;
    }

    @Override
    @Transactional
    public Long saveImage(SaveImageRequestDto saveImageRequestDto) throws Exception {
        try {
            Image image = null;
            if(saveImageRequestDto.getImageId()!=null) {
                image = imageDao.fetchById(saveImageRequestDto.getImageId());
            }
            if (image == null) {
                image = new Image();
            }

            if (saveImageRequestDto.getImage() != null)
                image.setImage(saveImageRequestDto.getImage());

            if (saveImageRequestDto.getDescription() != null)
                image.setDescription(saveImageRequestDto.getDescription());

            if (saveImageRequestDto.getTitle() != null)
                image.setTitle(saveImageRequestDto.getTitle());

            if (saveImageRequestDto.getAccountId() != null)
                image.setAccountId(saveImageRequestDto.getAccountId());

            imageDao.save(image);

            // Add tags
            if (!saveImageRequestDto.getTags().isEmpty()) {
                List<ImageTags> imageTagses = new ArrayList<>();
                for(String tag : saveImageRequestDto.getTags()) {
                    ImageTags imageTags = new ImageTags();
                    imageTags.setSourceImage(image);
                    imageTags.setTag(tag);
                    imageTagService.saveImageTag(imageTags);
                    imageTagses.add(imageTags);
                }
                image.setTags(imageTagses);
            }

            imageDao.save(image);

            return image.getImageId();
        } catch (Exception e) {
            log.error("Error in saving Image " + e);
            throw e;
        }
    }

    @Transactional
    public String getImageData(Long imageId) {
        Image image = getImage(imageId);
        if(image == null)
            return null;
        return image.getImage();
    }

    @Override
    @Transactional
    public ImageMetaData getImageMetaData(Long imageId) {
        List<Image> images = imageDao.getMetaData(imageId, null);
        if(images.size() == 0)
            return null;
        return new ImageMetaData(images.get(0));
    }

    @Override
    @Transactional
    public List<ImageMetaData> getImageMetaData(String accountId) {
        List<Image> imageList = imageDao.getMetaData(null, accountId);
        List<ImageMetaData> metaDataList = new ArrayList<>();
        for(Image image : imageList){
            metaDataList.add(new ImageMetaData(image));
        }
        return metaDataList;
    }

    @Override
    public Image getImage(String accountId) {
        return null;
    }

    @Override
    @Transactional
    public Image getImage(Long imageId) {
        try {
            return imageDao.fetchById(imageId);
        } catch (Exception e) {
            log.error("Some error while fetching for " + imageId, e);
            throw new ResponseException("Error while fetching for " + imageId + ". " + e.getMessage(), e);
        }
    }

    @Override
    public List<Image> findByAccountId(String accountId) {
        return imageDao.findByAccountId(accountId);
    }

    @Override
    @Transactional
    public SearchResponseDto search(String searchText) {
        // Search Tags
        List<ImageTags> imageTags = imageTagService.searchImageTags(searchText);
        List<String> tagStrings = imageTags.stream().map(imageTag -> imageTag.getTag()).collect(Collectors.toList());
        tagStrings = new ArrayList<>(new HashSet<>(tagStrings));

        // Search Users
        SearchUserRequestDto searchUserRequestDto = new SearchUserRequestDto();
        searchUserRequestDto.setUserLike(searchText);
        List<User> users = userService.searchUser(searchUserRequestDto);

        SearchResponseDto searchResponseDto = new SearchResponseDto();
        searchResponseDto.setTags(tagStrings);
        List<UserResponseDto> userResponseDtos = new ArrayList<>();
        if(users!=null && users.size()>0) {
            for(User user : users) {
                UserResponseDto userResponseDto = new UserResponseDto(user);
                userResponseDtos.add(userResponseDto);
            }
            searchResponseDto.setUsers(userResponseDtos);
        }
        return searchResponseDto;
    }

    @Override
    public ProfileDetails getProfileDetails(String accountId) throws Exception {

        ProfileDetails profileDetails = new ProfileDetails();

        // get user details
        SearchUserRequestDto searchUserRequestDto = new SearchUserRequestDto();
        searchUserRequestDto.setAccountId(accountId);
        List<User> users = userService.searchUser(searchUserRequestDto);
        if(users==null || users.size()==0)
            throw new ResponseException("User not found",500);
        UserResponseDto userResponseDto = new UserResponseDto(users.get(0));
        profileDetails.setUser(userResponseDto);

        // get all images of user
        Criterion criterion = Restrictions.eq("accountId", accountId);
        List<Image> images = imageDao.findByCriteria(criterion);

        // filter owned images
        List<Image> ownedImages = images.stream().filter(image -> image.getIsCloned().equals(false)).collect(Collectors.toList());
        profileDetails.setOwnedImages(ownedImages);

        // filter cloned images
        List<Image> clonedImages = images.stream().filter(image -> image.getIsCloned().equals(true)).collect(Collectors.toList());
        profileDetails.setClonedImages(clonedImages);

        // get Pull Request
        if(ownedImages!=null && ownedImages.size()>0) {
            List<ImageRelation> imageRelations = imageRelationService.getImageRelationsForSourceImageIds(ownedImages.stream().map(image -> image.getImageId()).collect(Collectors.toList()));
            imageRelations.removeIf(imageRelation -> !imageRelation.getApprovalStatus().equals(ApprovalStatusEnum.PENDING));
            List<PullRequest> pullRequests = new ArrayList<>();
            if(imageRelations!=null && imageRelations.size()>0) {
                List<Long> clonedImagesByOthersId = imageRelations.stream().map(imageRelation -> imageRelation.getClonedImage()).collect(Collectors.toList());
                Criterion idCriterion = Restrictions.in("imageId", clonedImagesByOthersId);
                List<Image> clonedImagesByOthers = imageDao.findByCriteria(idCriterion);
                List<String> accountIds = clonedImagesByOthers.stream().map(image -> image.getAccountId()).collect(Collectors.toList());
                SearchUserRequestDto userRequestDto = new SearchUserRequestDto();
                userRequestDto.setAccountIds(accountIds);
                List<User> usersList = userService.searchUser(userRequestDto);
                int index = 0;
                for(Image clonedImage : clonedImagesByOthers) {
                    PullRequest pullRequest = new PullRequest();
                    pullRequest.setImage(clonedImage);
                    pullRequest.setSender(new UserResponseDto(usersList.get(index++)));
                    pullRequests.add(pullRequest);
                }
            }
            profileDetails.setPullRequests(pullRequests);
        }
        return profileDetails;
    }

    @Override
    public Long cloneImage(Long imageId, String accountId) throws Exception {
        Image image = imageDao.fetchById(imageId);
        Image clonedImage = new Image();
        clonedImage.setAccountId(accountId);
        clonedImage.setTitle(image.getTitle());
        clonedImage.setDescription(image.getDescription());
        clonedImage.setImage(image.getImage());
        clonedImage.setIsCloned(true);
        imageDao.save(clonedImage);
        List<ImageTags> newImageTags = new ArrayList<>();
        for(ImageTags imageTags : image.getTags()) {
            ImageTags newImageTag = new ImageTags();
            newImageTag.setTag(imageTags.getTag());
            newImageTag.setSourceImage(clonedImage);
            imageTagService.saveImageTag(newImageTag);
            newImageTags.add(newImageTag);
        }
        clonedImage.setTags(newImageTags);
        imageDao.save(clonedImage);

        ImageRelation imageRelation = new ImageRelation();
        imageRelation.setApprovalStatus(ApprovalStatusEnum.NEW);
        imageRelation.setClonedImage(clonedImage.getImageId());
        imageRelation.setId(imageId);

        imageRelationService.saveImageRelation(imageRelation);
        return clonedImage.getImageId();
    }
}
