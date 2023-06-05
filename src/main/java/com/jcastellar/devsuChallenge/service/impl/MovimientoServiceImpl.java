package com.jcastellar.devsuChallenge.service.impl;

import com.jcastellar.devsuChallenge.dto.MovimientoDTO;
import com.jcastellar.devsuChallenge.entity.Cuenta;
import com.jcastellar.devsuChallenge.entity.Movimiento;
import com.jcastellar.devsuChallenge.repository.ClienteRepository;
import com.jcastellar.devsuChallenge.repository.CuentaRepository;
import com.jcastellar.devsuChallenge.repository.MovimientoRepository;
import com.jcastellar.devsuChallenge.service.MovimientoService;
import com.jcastellar.devsuChallenge.utility.excepciones.NoEncontrado;
import com.jcastellar.devsuChallenge.utility.excepciones.PeticionErronea;
import com.jcastellar.devsuChallenge.utility.mapper.MovimientoMapper;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

@Service
public class MovimientoServiceImpl implements MovimientoService {

  private final String SND = "Saldo no disponible";
  private final String CDE = "Cupo diario excedido";

  private final MovimientoRepository movimientoRepository;
  private final MovimientoMapper movimientoMapper;
  private final CuentaRepository cuentaRepository;
  private final ClienteRepository clienteRepository;

  @Autowired
  public MovimientoServiceImpl(MovimientoRepository movimientoRepository,
      MovimientoMapper movimientoMapper, CuentaRepository cuentaRepository,
      ClienteRepository clienteRepository) {
    this.movimientoRepository = movimientoRepository;
    this.movimientoMapper = movimientoMapper;
    this.cuentaRepository = cuentaRepository;
    this.clienteRepository = clienteRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<MovimientoDTO> getMovimientos() {
    List<Movimiento> movimientos = movimientoRepository.findAll();
    return movimientos.stream().map(movimientoMapper::movimientoToMovimientoDTO).collect(
        Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public MovimientoDTO getMovimiento(Long id) {
    return movimientoRepository.findById(id).map(movimientoMapper::movimientoToMovimientoDTO)
        .orElse(null);
  }

  @Override
  @Transactional
  public MovimientoDTO createMovimiento(MovimientoDTO movimientoDTO) {
    Optional<Cuenta> cuentaOpt = cuentaRepository.findByNumeroCuenta(
        movimientoDTO.getCuenta().getNumeroCuenta());
    double saldoTotal = 0;
    if (cuentaOpt.isPresent()) {
      if (cuentaOpt.get().getMovimientos().size() == 0) {
        saldoTotal = hacerMovimiento(movimientoDTO.getTipoMovimiento().getValue(),
            cuentaOpt.get().getSaldoInicial(), movimientoDTO.getValor());
      } else {
        double saldoUltimoMovimiento = getUltimoMovimiento(cuentaOpt.get().getMovimientos());
        saldoTotal = hacerMovimiento(movimientoDTO.getTipoMovimiento().toString(),
            saldoUltimoMovimiento, movimientoDTO.getValor());
      }
      if (saldoTotal < 0) {
        throw new PeticionErronea(SND);
      }

      Movimiento movimiento = movimientoMapper.movimientoDTOToMovimiento(movimientoDTO);
      movimiento.setFecha(LocalDate.now());
      movimiento.setCuenta(cuentaOpt.get());
      movimiento.setSaldo(saldoTotal);

      if (validarSaldoExcedido(cuentaOpt.get().getMovimientos(), movimiento)) {
        throw new PeticionErronea(CDE);
      }

      final Movimiento nuevoMovimiento = movimientoRepository.save(movimiento);
      return movimientoMapper.movimientoToMovimientoDTO(nuevoMovimiento);
    }
    throw new NoEncontrado("Cuenta no encontrada");
  }

  @Override
  @Transactional
  public void deleteById(Long id) {
    Optional<Movimiento> movimientoOpt = movimientoRepository.findById(id);
    if (movimientoOpt.isPresent()) {
      movimientoRepository.deleteById(id);
    } else {
      throw new NoEncontrado("Movimiento no encontrado");
    }
  }

  @Override
  @Transactional
  public MovimientoDTO updateMovimiento(Long id, MovimientoDTO movimientoDTO) {
    Optional<Movimiento> movimientoOpt = movimientoRepository.findById(id);
    if (movimientoOpt.isPresent()) {
      double saldoTotal = 0;
      Movimiento movimiento = movimientoMapper.movimientoDTOToMovimiento(movimientoDTO);
      Movimiento movimientoActualizado = movimientoRepository.findById(id).get();
      //movimientoActualizado.setFecha(LocalDate.now());
      movimientoActualizado.setTipoMovimiento(movimiento.getTipoMovimiento());
      movimientoActualizado.setValor(movimiento.getValor());

      Optional<Cuenta> cuentaOpt = cuentaRepository.findByNumeroCuenta(
          movimientoDTO.getCuenta().getNumeroCuenta());
      saldoTotal = ((movimiento.getValor() - movimientoOpt.get().getValor())
          + getUltimoMovimiento(cuentaOpt.get().getMovimientos()));
      movimientoActualizado.setSaldo(saldoTotal);

      movimientoRepository.save(movimientoActualizado);
      return movimientoMapper.movimientoToMovimientoDTO(movimientoActualizado);
    }
    throw new NoEncontrado("Movimiento no encontrado");
  }

  @Override
  @Transactional
  public MovimientoDTO actualizacionParcialByFields(Long id, Map<String, Object> fields) {
    Optional<Movimiento> movimientoOpt = movimientoRepository.findById(id);
    if (movimientoOpt.isPresent()) {
      Optional<Movimiento> movimientoActualizado = movimientoRepository.findById(id);
      fields.forEach((key, value) -> {
        Field field = ReflectionUtils.findField(Movimiento.class, key);
        field.setAccessible(true);
        ReflectionUtils.setField(field, movimientoActualizado.get(), value);
      });
      movimientoRepository.save(movimientoActualizado.get());
      MovimientoDTO movimientoDTO = movimientoMapper.movimientoToMovimientoDTO(
          movimientoActualizado.get());
      return movimientoDTO;
    } else {
      throw new NoEncontrado("Movimiento no encontrado");
    }
  }

  private double hacerMovimiento(String tipoMovimiento, double saldo, double valor) {
    double saldoTotal = 0;
    if (tipoMovimiento.equalsIgnoreCase("Retiro")) {
      saldoTotal = saldo - valor;
    } else if (tipoMovimiento.equalsIgnoreCase("Deposito")) {
      saldoTotal = saldo + valor;
    }
    return saldoTotal;
  }

  public double getUltimoMovimiento(List<Movimiento> movimientos) {
    Movimiento movimiento = movimientos.stream().reduce((first, second) -> second).orElse(null);
    return Objects.nonNull(movimiento) ? movimiento.getSaldo() : 0;
  }

  private boolean validarSaldoExcedido(List<Movimiento> movimientoList, Movimiento movimiento) {
    AtomicReference<Double> saldoMaxDia = new AtomicReference<>((double) 0);
    List<Movimiento> movimientoLista = movimientoList.stream()
        .filter(m -> m.getFecha().isEqual(movimiento.getFecha())
            && m.getTipoMovimiento().getValue().equalsIgnoreCase("Retiro")).toList();

    movimientoLista.forEach(mov -> {
      saldoMaxDia.set(saldoMaxDia.get() + mov.getValor());
    });

    if (movimiento.getTipoMovimiento().getValue().equalsIgnoreCase("Retiro")) {
      saldoMaxDia.set(saldoMaxDia.get() + movimiento.getValor());
    }
    return saldoMaxDia.get() >= 1000 ? Boolean.TRUE : Boolean.FALSE;
  }
}