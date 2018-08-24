package com.alpha.marketplace.models;

import com.google.cloud.storage.BlobId;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "extensions")
public class Extension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "extension_id")
    private long id;

    @Column(name = "extension_name")
    private String name;

    @Column(name = "description", nullable = false, length = 5000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User publisher;

    @Column(name = "downloads", nullable = false)
    private int downloads;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "blob_id")
    private BlobId blobId;

    @ManyToMany(
            fetch = FetchType.EAGER,
            cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE,
    })
    @JoinTable(name = "tagged_extensions",
            joinColumns = @JoinColumn(name = "extension_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;

    @Column(name = "publish_date", nullable = false)
    private Date addedOn;

    @Column(name = "update_date")
    private Date latestUpdate;

    @Column(name = "is_approved", nullable = false)
    private boolean isApproved;

    @Column(name = "picture", nullable = false)
    private String picURI;

    @Column(name = "dl_uri", nullable = false)
    private String dlURI;

    @Column(name = "repo_url", nullable = false)
    private String repoURL;

    @OneToOne
    private GitHubInfo gitHubInfo;

    //TODO add GitHub API fields for pull requests, open issues and latest commit.

    public Extension(){
        setDownloads(0);
        setAddedOn(new Date());
        setApproved(false);
        setTags(new ArrayList<>());
        setPicURI("https://pbs.twimg.com/profile_images/932536730221133824/4XWcwfBt_400x400.jpg");
    }

    public Extension(
            String name,
            String description,
            User publisher,
            int downloads,
            String version,
            BlobId blobId,
            List<Tag> tags,
            Date addedOn,
            Date latestUpdate,
            boolean isApproved,
            String picURI,
            String dlURI,
            String repoURL,
            GitHubInfo gitHubInfo
        ) {
        this.name = name;
        this.description = description;
        this.publisher = publisher;
        this.downloads = downloads;
        this.version = version;
        this.blobId = blobId;
        this.tags = tags;
        this.addedOn = addedOn;
        this.latestUpdate = latestUpdate;
        this.isApproved = isApproved;
        this.picURI = picURI;
        this.dlURI = dlURI;
        this.repoURL = repoURL;
        this.gitHubInfo = gitHubInfo;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getPublisher() {
        return publisher;
    }

    public void setPublisher(User publisher) {
        this.publisher = publisher;
    }

    public int getDownloads() {
        return downloads;
    }

    public void setDownloads(int downloads) {
        this.downloads = downloads;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public BlobId getBlobId() {
        return blobId;
    }

    public void setBlobId(BlobId blobId) {
        this.blobId = blobId;
    }

    public Date getAddedOn() {
        return addedOn;
    }

    public void setAddedOn(Date addedOn) {
        this.addedOn = addedOn;
    }

    public Date getLatestUpdate() {
        return latestUpdate;
    }

    public void setLatestUpdate(Date latestUpdate) {
        this.latestUpdate = latestUpdate;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public String getPicURI() {
        return picURI;
    }

    public void setPicURI(String picURI) {
        this.picURI = picURI;
    }

    public String getDlURI() {
        return dlURI;
    }

    public void setDlURI(String dlURI) {
        this.dlURI = dlURI;
    }

    public String getRepoURL() {
        return repoURL;
    }

    public void setRepoURL(String repoURL) {
        this.repoURL = repoURL;
    }

    public GitHubInfo getGitHubInfo() {
        return gitHubInfo;
    }

    public void setGitHubInfo(GitHubInfo gitHubInfo) {
        this.gitHubInfo = gitHubInfo;
    }

    public void approve(){
        isApproved = true;
    }

}
