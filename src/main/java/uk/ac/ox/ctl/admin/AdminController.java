package uk.ac.ox.ctl.admin;

import org.springframework.beans.factory.annotation.Autowired;
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
    Tool getToolById(@PathVariable UUID id) throws Exception {
        return repository.findById(id)
            .orElseThrow(() -> new Exception("Tool with id ${id} not found."));
    }

    @PutMapping("/tools/{id}")
    Tool updateTool(@RequestBody Tool newTool, @PathVariable UUID id) throws Exception {
        return repository.findById(id).map(tool -> {
            newTool.setId(id);
            return repository.save(newTool);
        }).orElseThrow(() -> new Exception("Tool with id ${id} not found."));
    }

    @DeleteMapping("/tools/{id}")
    void deleteTool(@PathVariable UUID id) {
        repository.deleteById(id);
    }
}
