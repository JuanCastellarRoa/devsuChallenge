package com.jcastellar.devsuChallenge.utility.mapper;

import com.jcastellar.devsuChallenge.dto.ClienteDTO;
import com.jcastellar.devsuChallenge.entity.Cliente;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2023-06-05T14:46:24-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.2 (Oracle Corporation)"
)
@Component
public class ClienteMapperImpl implements ClienteMapper {

    @Override
    public Cliente clienteDTOToCliente(ClienteDTO clienteDTO) {
        if ( clienteDTO == null ) {
            return null;
        }

        Cliente cliente = new Cliente();

        cliente.setClienteId( clienteDTO.getClienteId() );
        cliente.setNombre( clienteDTO.getNombre() );
        cliente.setGenero( clienteDTO.getGenero() );
        cliente.setEdad( clienteDTO.getEdad() );
        cliente.setIdentificacion( clienteDTO.getIdentificacion() );
        cliente.setDireccion( clienteDTO.getDireccion() );
        cliente.setTelefono( clienteDTO.getTelefono() );
        cliente.setPassword( clienteDTO.getPassword() );
        cliente.setEstado( clienteDTO.getEstado() );

        return cliente;
    }

    @Override
    public ClienteDTO clienteToClienteDTO(Cliente cliente) {
        if ( cliente == null ) {
            return null;
        }

        ClienteDTO clienteDTO = new ClienteDTO();

        clienteDTO.setClienteId( cliente.getClienteId() );
        clienteDTO.setNombre( cliente.getNombre() );
        clienteDTO.setGenero( cliente.getGenero() );
        clienteDTO.setEdad( cliente.getEdad() );
        clienteDTO.setIdentificacion( cliente.getIdentificacion() );
        clienteDTO.setDireccion( cliente.getDireccion() );
        clienteDTO.setTelefono( cliente.getTelefono() );
        clienteDTO.setEstado( cliente.getEstado() );
        clienteDTO.setPassword( cliente.getPassword() );

        return clienteDTO;
    }
}
