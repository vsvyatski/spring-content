package internal.org.springframework.content.rest.contentservice;

import internal.org.springframework.content.rest.controllers.MethodNotAllowedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;

public interface ContentService {

    void getContent(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, Resource resource, MediaType resourceType)
            throws MethodNotAllowedException;

    void setContent(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, Resource source, MediaType sourceMimeType, Resource target)
            throws IOException, MethodNotAllowedException;

    void unsetContent(Resource resource)
            throws MethodNotAllowedException;
}
