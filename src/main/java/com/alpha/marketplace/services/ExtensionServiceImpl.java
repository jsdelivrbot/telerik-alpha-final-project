package com.alpha.marketplace.services;

import com.alpha.marketplace.models.Extension;
import com.alpha.marketplace.models.GitHubInfo;
import com.alpha.marketplace.models.Tag;
import com.alpha.marketplace.models.User;
import com.alpha.marketplace.models.binding.ExtensionBindingModel;
import com.alpha.marketplace.repositories.base.*;
import com.alpha.marketplace.services.base.ExtensionService;
import com.alpha.marketplace.utils.Utils;
import com.google.cloud.storage.BlobId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExtensionServiceImpl implements ExtensionService {

    private final String git = "https://github.com/";

    private final ExtensionRepository repository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final CloudExtensionRepository cloudExtensionRepository;
    private final ModelMapper mapper;
    private final GitHubRepository gitHubRepository;

    private List<Extension> all;
    private List<Extension> approved;
    private List<Extension> latest;
    private List<Extension> mostPopular;
    private List<Extension> unApproved;
    private static Thread syncManager;

    @Autowired
    public ExtensionServiceImpl(
            ExtensionRepository repository,
            UserRepository userRepository,
            CloudExtensionRepository cloudExtensionRepository,
            TagRepository tagRepository,
            ModelMapper mapper,
            GitHubRepository gitHubRepository
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.cloudExtensionRepository = cloudExtensionRepository;
        this.tagRepository = tagRepository;
        this.mapper = mapper;
        this.gitHubRepository = gitHubRepository;
        this.all = repository.getAll();
        this.approved = new ArrayList<>();
        this.latest = new ArrayList<>();
        this.mostPopular = new ArrayList<>();
        this.unApproved = new ArrayList<>();
    }


    @Override
    public List<Extension> getMostPopular() {
        if (mostPopular.isEmpty()) {
            mostPopular = approved.stream()
                    .sorted(Comparator.comparing(Extension::getDownloads).reversed())
                    .limit(10)
                    .collect(Collectors.toList());
        }
        return mostPopular;
    }

    @Override
    public List<Extension> getAdminSelection() {
        //TODO implement admin selection
        return null;
    }

    @Override
    public List<Extension> getLatest() {
        if (latest.isEmpty()) {
            latest = approved.stream()
                    .sorted(Comparator.comparing(Extension::getAddedOn).reversed())
                    .limit(10)
                    .collect(Collectors.toList());
        }
        return latest;
    }

    @Override
    public void createExtension(ExtensionBindingModel model, BindingResult errors) {

        User publisher = currentUser();
        BlobId blobid = null;
        Extension extension = mapper.map(model, Extension.class);
        extension.setPublisher(publisher);

        if(!validateRepoUrl(model.getRepositoryUrl())){
            errors.addError(new ObjectError("link", "Repository URL is invalid"));
            return;
        }

        extension.setRepoURL(model.getRepositoryUrl());

        if(model.getFile().isEmpty() || model.getPic().isEmpty()){
            errors.addError(new ObjectError("noFile", "No file received."));
        }

        String fileext = model.getFile().getOriginalFilename();
        String picext = model.getFile().getOriginalFilename();
        fileext = fileext.substring(fileext.lastIndexOf("."));
        picext = picext.substring(picext.lastIndexOf("."));

        try {
            blobid = cloudExtensionRepository.saveExtension(
                    String.valueOf(publisher.getId()),
                    extension.getName() + fileext,
                    model.getFile().getContentType(),
                    model.getFile().getBytes()
            );
            extension.setBlobId(blobid);
            extension.setDlURI(cloudExtensionRepository.getEXTENSION_URL_PREFIX() + blobid.getName());
            extension.setBlobId(blobid);
            String picURI = cloudExtensionRepository.saveExtensionPic(
                    String.valueOf(publisher.getId()),
                    extension.getName() + picext,
                    model.getPic().getContentType(),
                    model.getPic().getBytes()
            );
            extension.setPicURI(picURI);
        }catch(IOException e){
            //Could replace this with a log entry
            System.out.println(e.getMessage());
            if(blobid != null){
                cloudExtensionRepository.delete(blobid);
            }
            errors.addError(new ObjectError("fileProblem", "Failed to upload file"));
            return;
        }

        repository.save(extension);
        extension.setGitHubInfo(new GitHubInfo());
        extension.getGitHubInfo().setParent(extension);
        Utils.updateGithubInfo(extension.getGitHubInfo());
        gitHubRepository.save(extension.getGitHubInfo());
        extension.setTags(handleTags(model.getTagString(), extension));
        repository.update(extension);
        reloadLists();
    }

    @Override
    public Extension getById(int id) {
        if (id < 0) {
            return null;
        }
        return repository.getById(id);
    }

    @Override
    public List<Extension> getAllApproved() {
        if (approved.isEmpty()) {
            approved =  all.stream()
                    .filter(Extension::isApproved).collect(Collectors.toList());
        }
        return approved;
    }

    @Override
    public void approveExtensionById(int id) {

        Extension extension = getById(id);
        extension.approve();
        repository.update(extension);
    }

    @Override
    public Extension getByName(String name) {
        return repository.getByName(name);
    }


    //TODO add logging for github sync
    @Override
    public void sync(int id) {
        Extension extension = getById(id);
        GitHubInfo info = extension.getGitHubInfo();
        Date currentTime = new Date();
        System.out.println("[" + currentTime + "]" + "Admin syncing for " + extension.getName() + ":");
        Utils.updateGithubInfo(info);
        gitHubRepository.update(info);
        System.out.println("--Updated info for " + extension.getName());
    }

    @Override
    public void delete(int id) {
        repository.delete(id);
        reloadLists();
    }

    public void syncAll() {
        Date currentTime = new Date();
        List<Extension> extensions = getAllApproved();
        System.out.println("[" + currentTime + "]" + "Syncing:");
        for (Extension e : extensions) {
            if (e.getGitHubInfo() == null) {
                continue;
            }
            GitHubInfo ginfo = e.getGitHubInfo();
            Utils.updateGithubInfo(ginfo);
            gitHubRepository.update(ginfo);
            System.out.println("--Updated info for " + e.getName());
        }
        reloadLists();
    }

    @Override
    public void reloadLists() {
        all.clear();
        approved.clear();
        mostPopular.clear();
        latest.clear();
        unApproved.clear();
        all = repository.getAll();
        approved = getAllApproved();
        mostPopular = getMostPopular();
        latest = getLatest();
        unApproved = getUnapproved();
        System.out.println("Lists reloaded");
    }

    @Override
    public List<Extension> getUnapproved() {
        if (unApproved.isEmpty()) {
            unApproved =  all.stream()
                    .filter(extension -> !extension.isApproved()).collect(Collectors.toList());
        }
        return unApproved;
    }


    @Override
    public boolean isUserPublisherOrAdmin(Extension extension) {
        if(Utils.userIsAnonymous()){
            return false;
        }
        User u = currentUser();
        return u.isAdmin() || u.isPublisher(extension);
    }

    @Override
    public User currentUser() {
        if(Utils.userIsAnonymous()){
            return null;
        }
        UserDetails user = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
      return userRepository.findByUsername(user.getUsername());
    }

    private boolean validateRepoUrl(String repo){
        if(!repo.startsWith(git)){
            return false;
        }
        repo = repo.substring(Utils.GITHUB_URL_PREFIX.length());
        String[] words = repo.split("/");
        return words.length == 2;
    }

    private Set<Tag> handleTags(String tagString, Extension extension){
        String [] tagArray = tagString.split(", ");
        Set<Tag> tags = new HashSet<>();

        for (String tag: tagArray) {
            Tag t = tagRepository.findByName(tag);

            if(t == null){
                Tag newTag = new Tag(tag);
                tagRepository.saveTag(newTag);
            }
            else {
                t.getTaggedExtensions().add(extension);

                tags.add(t);
            }
        }
        return tags;
    }

    public void setSync(long period){
        if(syncManager != null){
            syncManager.interrupt();
        }
        syncManager = new Thread(() -> {
            while(true){
                syncAll();
                try {
                    Thread.sleep(period);
                } catch (InterruptedException e) {
                    System.out.println("Thread interrupted");
                }
            }
        });
        syncManager.start();
    }

}
