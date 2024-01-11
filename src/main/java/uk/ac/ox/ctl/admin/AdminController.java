package uk.ac.ox.ctl.admin;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.model.ToolRegistration;
import uk.ac.ox.ctl.model.ToolRegistrationLti;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final ToolRepository repository;

    public AdminController(ToolRepository repository) {
        this.repository = repository;
    }

    @GetMapping(value = "/tools")
    public Iterable<Tool> getAllTools() {
        return repository.findAll();
    }

    @GetMapping("/tools/{id}")
    ResponseEntity<Tool> getToolById(@PathVariable UUID id) {
        return repository.findById(id).map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/tools/ltiClientId:{id}")
    ResponseEntity<Tool> getToolByLtiClientId(@PathVariable String id) {
        return repository.findToolByLtiClientId(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/tools/proxyClientId:{id}")
    ResponseEntity<Tool> getToolByProxyClientId(@PathVariable String id) {
        return repository.findToolByProxyClientId(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/tools/ltiRegistrationId:{id}")
    ResponseEntity<Tool> getToolByLtiRegistrationId(@PathVariable String id) {
        return repository.findToolByLtiRegistrationId(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/tools/proxyRegistrationId:{id}")
    ResponseEntity<Tool> getToolByProxyRegistrationId(@PathVariable String id) {
        return repository.findToolByProxyRegistrationId(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/tools")
    ResponseEntity<Tool> createTool(@RequestBody Tool newTool) {
        return ResponseEntity.ok(repository.save(newTool));
    }


    @PutMapping("/tools/{id}")
    ResponseEntity<Tool> updateTool(@RequestBody Tool newTool, @PathVariable UUID id) {
        return repository.findById(id).map(tool -> {
            ToolRegistration newLti = newTool.getLti();
            if(newLti == null){
                tool.setLti(null);
            }else{
                ToolRegistration existingLti = tool.getLti();
                if(existingLti == null){
                    tool.setLti(new ToolRegistrationLti());
                    existingLti = tool.getLti();
                }
                setToolRegistrationFields(existingLti, newLti);
            }

            ToolRegistration newProxy = newTool.getProxy();
            if(newProxy == null){
                tool.setProxy(null);
            }else{
                ToolRegistration existingProxy = tool.getProxy();
                if(existingProxy == null){
                    tool.setProxy(new ToolRegistrationProxy());
                    existingProxy = tool.getProxy();
                }
                setToolRegistrationFields(existingProxy, newProxy);
            }

            tool.setOrigins(newTool.getOrigins());
            tool.setSecret(newTool.getSecret());
            tool.setIssuer(newTool.getIssuer());
            tool.setNrpsAllowedRoles(newTool.getNrpsAllowedRoles());

            return ResponseEntity.ok(repository.save(tool));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private void setToolRegistrationFields(ToolRegistration existingTool, ToolRegistration newTool) {
        existingTool.setRegistrationId(newTool.getRegistrationId());
        existingTool.setClientName(newTool.getClientName());
        existingTool.setClientId(newTool.getClientId());
        existingTool.setClientSecret(newTool.getClientSecret());
        existingTool.setClientAuthenticationMethod(newTool.getClientAuthenticationMethod());
        existingTool.setAuthorizationGrantType(newTool.getAuthorizationGrantType());
        existingTool.setRedirectUri(newTool.getRedirectUri());
        existingTool.setScopes(newTool.getScopes());
        existingTool.setProviderDetails(newTool.getProviderDetails());
    }

    @DeleteMapping("/tools/{id}")
    ResponseEntity<Void> deleteTool(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleNoSuchElementFoundException(
            DataIntegrityViolationException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(exception.getMessage());
    }
}
