package internal.org.springframework.content.rest.controllers;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.contentservice.ContentService;
import internal.org.springframework.content.rest.contentservice.ContentServiceFactory;
import internal.org.springframework.content.rest.io.InputStreamResource;
import internal.org.springframework.content.rest.io.StoreResource;
import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToExportedContext;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;
import internal.org.springframework.content.rest.utils.HeaderUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.Date;

@ContentRestController
public class StoreRestController implements InitializingBean {

    private static final String STORE_REQUEST_MAPPING = "/{store}/**";

    private static final String HEADER_CONTENT_TYPE_NOT_MULTIPART =
        "content-type!=" + MediaType.MULTIPART_FORM_DATA_VALUE;

    private static final String HEADER_CONTENT_TYPE_MULTIPART =
        "content-type=" + MediaType.MULTIPART_FORM_DATA_VALUE;

    @Autowired
    ApplicationContext context;

    @Autowired(required = false)
    private Repositories repositories;

    @Autowired
    private Stores stores;

    @Autowired(required = false)
    private RepositoryInvokerFactory repoInvokerFactory;

    @Autowired
    private RestConfiguration config;

    @Autowired
    private StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;

    @Autowired
    private MappingContext mappingContext;

    @Autowired
    private ContentPropertyToExportedContext exportedMappingContext;

    private ContentServiceFactory contentServiceFactory;

    public StoreRestController() {
    }

    protected static long resolveLastModified(StoreResource storeResource) {
        long lastModified = -1L;
        try {
            lastModified = storeResource.lastModified();
        } catch (FileSystemNotFoundException | IOException ignored) {
        }
        return lastModified;
    }

    private static MediaType mediaTypeOfMultipartFile(MultipartFile multiPart) {
        String contentType = multiPart.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.GET)
    public void getContent(HttpServletRequest request,
                           HttpServletResponse response,
                           @RequestHeader HttpHeaders headers,
                           StoreResource storeResource)
        throws MethodNotAllowedException {

        if (storeResource == null || !storeResource.exists()) {
            throw new ResourceNotFoundException();
        }

        if (new ServletWebRequest(request, response).checkNotModified(storeResource.getETag() != null ? storeResource.getETag().toString() : null, resolveLastModified(storeResource))) {
            return;
        }

        ContentService contentService = contentServiceFactory.getContentService(storeResource);

        contentService.getContent(request, response, headers, storeResource, storeResource.getMimeType());
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.PUT, headers = {
        HEADER_CONTENT_TYPE_NOT_MULTIPART, "accept!=application/hal+json"})
    @ResponseBody
    public void putContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers, StoreResource storeResource)
        throws IOException, MethodNotAllowedException {

        ContentService contentService = contentServiceFactory.getContentService(storeResource);

        handleMultipart(request, response, headers,
            contentService,
            new InputStreamResource(request.getInputStream(), null),
            headers.getContentType(),
            storeResource,
            storeResource.getETag());
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.PUT, headers = HEADER_CONTENT_TYPE_MULTIPART)
    @ResponseBody
    public void putMultipartContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
                                    @RequestParam("file") MultipartFile multiPart, StoreResource storeResource)
        throws IOException, MethodNotAllowedException {

        ContentService contentService = contentServiceFactory.getContentService(storeResource);

        headers.setContentLength(multiPart.getSize());

        handleMultipart(request, response, headers,
            contentService,
            multiPart.getResource(),
            mediaTypeOfMultipartFile(multiPart),
            storeResource,
            storeResource.getETag());
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.POST, headers = HEADER_CONTENT_TYPE_MULTIPART)
    @ResponseBody
    public void postMultipartContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers,
                                     @RequestParam("file") MultipartFile multiPart, StoreResource storeResource)
        throws IOException, MethodNotAllowedException {

        ContentService contentService = contentServiceFactory.getContentService(storeResource);

        headers.setContentLength(multiPart.getSize());

        handleMultipart(request, response, headers,
            contentService,
            new InputStreamResource(multiPart.getInputStream(), multiPart.getOriginalFilename()),
            mediaTypeOfMultipartFile(multiPart),
            storeResource,
            storeResource.getETag());

    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.POST, headers = {HEADER_CONTENT_TYPE_NOT_MULTIPART})
    @ResponseBody
    public void postContent(HttpServletRequest request, HttpServletResponse response, @RequestHeader HttpHeaders headers, StoreResource storeResource)
        throws IOException, MethodNotAllowedException {

        ContentService contentService = contentServiceFactory.getContentService(storeResource);

        handleMultipart(request, response, headers,
            contentService,
            new InputStreamResource(request.getInputStream(), null),
            headers.getContentType(),
            storeResource,
            storeResource.getETag());
    }

    @RequestMapping(value = STORE_REQUEST_MAPPING, method = RequestMethod.DELETE, headers = "accept!=application/hal+json")
    public void deleteContent(@RequestHeader HttpHeaders headers, HttpServletResponse response, StoreResource storeResource)
        throws MethodNotAllowedException {

        if (!storeResource.exists()) {
            throw new ResourceNotFoundException();
        } else {
            HeaderUtils.evaluateHeaderConditions(headers, storeResource.getETag() != null ? storeResource.getETag().toString() : null, new Date(resolveLastModified(storeResource)));
        }

        ContentService contentService = contentServiceFactory.getContentService(storeResource);
        contentService.unsetContent(storeResource);

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    protected void handleMultipart(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers,
                                   ContentService contentService, Resource source, MediaType sourceMimeType,
                                   StoreResource target, Object targetETag)
        throws IOException, MethodNotAllowedException {

        boolean isNew = false;

        HeaderUtils.evaluateHeaderConditions(headers, targetETag != null ? targetETag.toString() : null, new Date(resolveLastModified(target)));
        if (!target.exists()) {
            isNew = true;
        }

        contentService.setContent(request, response, headers, source, sourceMimeType, target);

        if (isNew) {
            response.setStatus(HttpStatus.CREATED.value());
        } else {
            response.setStatus(HttpStatus.OK.value());
        }
    }

    @Override
    public void afterPropertiesSet() {

        try {
            this.repositories = context.getBean(Repositories.class);
        } catch (BeansException be) {
            this.repositories = new Repositories(context);
        }

        if (this.repoInvokerFactory == null) {
            this.repoInvokerFactory = new DefaultRepositoryInvokerFactory(this.repositories);
        }

        contentServiceFactory = new ContentServiceFactory(config, repositories, repoInvokerFactory, stores, mappingContext, exportedMappingContext, byteRangeRestRequestHandler);
    }
}
