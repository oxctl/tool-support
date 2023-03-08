package uk.ac.ox.ctl.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.model.ToolRegistration;
import uk.ac.ox.ctl.model.ToolRegistrationLti;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private ToolRepository repository;

    @PostMapping("/tools")
    Tool createTool(@RequestBody Tool newTool) {
        return repository.save(newTool);
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
            tool.setSign(newTool.getSign());
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
}
