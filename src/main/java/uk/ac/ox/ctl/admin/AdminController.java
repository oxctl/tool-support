package uk.ac.ox.ctl.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ox.ctl.model.Tool;
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
            newTool.setId(id);
            return ResponseEntity.ok(repository.save(newTool));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/tools/{id}")
    void deleteTool(@PathVariable UUID id) {
        repository.deleteById(id);
    }
}
