package br.edu.ifrs.tads.ppa.demo.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.edu.ifrs.tads.ppa.demo.model.NewUser;
import br.edu.ifrs.tads.ppa.demo.model.Profile;
import br.edu.ifrs.tads.ppa.demo.model.Role;
import br.edu.ifrs.tads.ppa.demo.model.User;
import br.edu.ifrs.tads.ppa.demo.repository.RoleRepository;
import br.edu.ifrs.tads.ppa.demo.repository.UserRepository;

public class UserService {
 
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private Set<String> defaultRoles;

    public UserService(
            UserRepository userRepository, 
            RoleRepository roleRepository,
            @Value("${app.user.default.roles}") Set<String> defaultRoles) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;   
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.defaultRoles = defaultRoles;
    }

    public void createUser(NewUser newUser){
        validateNewUser(newUser);

        User user = new User();
        
        user.setEmail(newUser.email());
        user.setHandle(newUser.handle() != null ? newUser.handle() : generateHandle(newUser.email()));
        user.setPassword(passwordEncoder.encode(newUser.password()));
        
        Set<Role> roles = new HashSet<>();
        
        roles.addAll(roleRepository.findByNameIn(defaultRoles));

        Set<Role> additionalRoles = roleRepository.findByNameIn(newUser.roles());
        if (additionalRoles.size() != newUser.roles().size()) {
            throw new IllegalArgumentException("Alguns papéis não existem");
        }

        if (roles.isEmpty()) {
            throw new IllegalArgumentException("O usuário deve ter pelo menos um papel");
        }

        user.setRoles(roles);

        Profile profile = new Profile();
        
        profile.setName(newUser.name());
        profile.setCompany(newUser.company());
        profile.setType(newUser.type() != null ? newUser.type() : Profile.AccountType.FREE);

        profile.setUser(user);
        user.setProfile(profile);

        userRepository.save(user); 
    }

    private void validateNewUser(NewUser newUser) {
        if (newUser.email() == null || newUser.password() == null) {
            throw new IllegalArgumentException("Email e senha são obrigatórios");
        }

        if (newUser.email().isEmpty() || newUser.password().isEmpty()) {
            throw new IllegalArgumentException("Email e senha não podem estar vazios");
        }

        if (!newUser.email().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Email não é válido");
        }

        if (!newUser.password().matches("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$")) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres e conter pelo menos uma letra e um número");
        }
        
        userRepository.findByEmail(newUser.email())
            .ifPresent(user -> {
                throw new IllegalArgumentException("Usuário com o email " + newUser.email() + " já existe");
            });  
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }
    
    private String generateHandle(String email) {
        String[] parts = email.split("@");
        String handle = parts[0];
        int i = 1;
        while (userRepository.existsByHandle(handle)) {
            handle = parts[0] + i++;
        }
        return handle;
    }
}




