package tk.jaooo.osmanager.services;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tk.jaooo.osmanager.model.Tecnico;
import tk.jaooo.osmanager.repository.TecnicoRepository;

import java.util.ArrayList;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private final TecnicoRepository tecnicoRepository;

    public MyUserDetailsService(TecnicoRepository tecnicoRepository) {
        this.tecnicoRepository = tecnicoRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Tecnico tecnico = tecnicoRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        return new User(tecnico.getUsername(), tecnico.getPasswordHash(), new ArrayList<>());
    }
}
