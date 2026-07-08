/**
 * Módulo de inventario de ganado bovino (cattle-inventory).
 * 
 * <p>Este módulo es el componente central de Vacapp que proporciona funcionalidades 
 * completas para la gestión integral del inventario de ganado bovino, incluyendo:
 * <ul>
 *   <li>Registro único de identificación (arete único global)</li>
 *   <li>Genealogía completa (padre/madre/hijos)</li>
 *   <li>Trazabilidad de movimientos entre potreros</li>
 *   <li>Historial de pesos y ganancia diaria</li>
 *   <li>Eventos de salud (vacunaciones, tratamientos, partos)</li>
 *   <li>Cumplimiento regulatorio mexicano (Folio REEMO)</li>
 * </ul>
 * 
 * <h2>Arquitectura del Módulo</h2>
 * <p>El módulo sigue la arquitectura Spring Modulith con Clean Architecture:
 * 
 * <h3>API Pública</h3>
 * <ul>
 *   <li>{@code CattleService.java} - Único punto de entrada público para otros módulos</li>
 * </ul>
 * 
 * <h3>Capa Internal (Privada)</h3>
 * <p>Todo bajo el paquete {@code internal/} es privado e inaccesible desde otros módulos:
 * 
 * <h4>Domain Layer</h4>
 * <ul>
 *   <li>{@code domain/model/} - Entidades de negocio puras (Animal, enums, value objects)</li>
 *   <li>{@code domain/model/exceptions/} - Excepciones de dominio</li>
 *   <li>{@code domain/repository/} - Puertos de salida (interfaces)</li>
 * </ul>
 * 
 * <h4>Application Layer</h4>
 * <ul>
 *   <li>{@code application/usecases/animal/} - Casos de uso de animales</li>
 *   <li>{@code application/usecases/movement/} - Casos de uso de movimientos</li>
 *   <li>{@code application/usecases/weight/} - Casos de uso de pesos</li>
 *   <li>{@code application/usecases/health/} - Casos de uso de salud</li>
 *   <li>{@code application/usecases/commands/} - Comandos y resultados (records)</li>
 * </ul>
 * 
 * <h4>Infrastructure Layer</h4>
 * <ul>
 *   <li>{@code infrastructure/controllers/mobile/} - Controladores REST API (JSON)</li>
 *   <li>{@code infrastructure/controllers/web/} - Controladores MVC (Thymeleaf)</li>
 *   <li>{@code infrastructure/persistence/} - Adaptadores JPA (entities, repositories, mappers)</li>
 *   <li>{@code infrastructure/integration/} - Integración con otros módulos</li>
 *   <li>{@code infrastructure/cache/} - Configuración de caché</li>
 *   <li>{@code infrastructure/config/} - Configuración del módulo</li>
 * </ul>
 * 
 * <h2>Principios de Diseño</h2>
 * <ul>
 *   <li><b>Encapsulamiento estricto</b>: Solo CattleService es público</li>
 *   <li><b>Separación de capas</b>: Dominio no depende de infraestructura</li>
 *   <li><b>Dependency Inversion</b>: Domain define interfaces, Infrastructure implementa</li>
 *   <li><b>Immutability</b>: Records para DTOs y comandos</li>
 *   <li><b>Fail-fast</b>: Validación temprana con Bean Validation</li>
 * </ul>
 * 
 * @see mx.vacapp.cattle.CattleService
 * @since 1.0.0
 */
package mx.vacapp.cattle;
