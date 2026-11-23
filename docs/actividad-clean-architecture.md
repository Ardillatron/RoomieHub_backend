=
**PARTE 1. Identificar el núcleo**
=


Regla 1: La capacidad de una vivienda está limitada a un máximo de 6 personas


Por qué es negocio: Es una política inquebrantable del sistema de convivencia que debe cumplirse obligatoriamente independientemente de si se utiliza PostgreSQL, un archivo de texto.

Entidades involucradas: Sala (atributos: ID, nombre, código de acceso) y UsuarioSala (atributos: usuario_id, sala_id, rol).

-------------------------------------------------------------------------------------------------------------------------------------------------------------

Regla 2: Las tareas tendrán un valor numérico estricto de 1 a 10 de acuerdo a la dificultad.


Por qué es negocio: Define la lógica matemática para evaluar equitativamente el esfuerzo dentro de la casa, por lo que es una restricción pura que no depende de la interfaz de usuario ni del framework.

Entidades involucradas: Tarea (atributos: titulo, puntos_dificultad, estado actual).


-------------------------------------------------------------------------------------------------------------------------------------------------------------

Regla 3: Una deuda no se liquida hasta que todos los involucrados confirmen la recepción del pago.


Por qué es negocio: Previene fraudes en el hogar y rige la máquina de estados contable; esta validación por consenso existe sin importar si la base de datos está en línea o en modo offline.

Entidades involucradas: DeudaUsuario (atributos: monto real de la deuda, estado de la transacción).




 
=
**PARTE 2. Diseño por capas**
=

Regla 1: La capacidad de una vivienda está limitada a un máximo de 6 personas
La Entity con sus validaciones:
La entidad es Sala. Tendrá una validación interna (un método como validarCapacidad()) que se asegure de que la lista de UsuarioSala asociadas a esa sala no
tenga un tamaño mayor a 6. Si es mayor o igual a 6 y se intenta agregar uno más, la entidad arroja una excepción

El Use Case que la ejecuta:
El Use Case es UnirseASalaUseCase.

Entrada: usuarioId y codigoAcceso.

Salida: Confirmación de éxito o un mensaje de error.

Pasos: 
1) Usar el repositorio para buscar la Sala por el código de acceso. 
2) La sala verifica su capacidad (Regla 1). 
3) Si hay lugar, se crea el vínculo UsuarioSala asignando el rol de INTEGRANTE.
4) 4) Usar el repositorio para guardar los cambios en la base de datos.

La interfaz del Repository:
ISalaRepository. Necesitará los métodos buscarPorCodigoAcceso y guardar.

El Adapter que la implementaría:
Un adaptador de base de datos. Específicamente, PostgresSalaAdapter que se conecta a PostgreSQL para ejecutar las consultas reales.


-------------------------------------------------------------------------------------------------------------------------------------------------------------
Regla 2: Las tareas tendrán un valor numérico estricto de 1 a 10 (BR-03)
La Entity con sus validaciones:
La entidad es Tarea. En su constructor, o en el método setPuntosDificultad(), tendrá la validación: if (puntos < 1 || puntos > 10) throw new IllegalArgumentException("Los puntos deben estar entre 1 y 10").

El Use Case que la ejecuta:
El Use Case es CrearTareaUseCase (CU-08).

Entrada: salaId, creadorId (Master), titulo, y puntosDificultad.

Salida: La nueva Tarea creada o un error de validación.

Pasos: 
1) Validar que el creador sea Administrador. 
2) Instanciar la entidad Tarea, pasándole los datos. La entidad validará automáticamente la regla de los puntos. 
3) Usar el repositorio para guardar la tarea.

La interfaz del Repository:
ITareaRepository. Necesitará el método guardarTarea.

El Adapter que la implementaría:
Igual que el anterior, un adaptador de BD  usando Spring Data JPA para insertar el registro en la tabla tareas.



-------------------------------------------------------------------------------------------------------------------------------------------------------------
Regla 3: El sistema impone un máximo de 2 salas creadas por usuario 
La Entity con sus validaciones:
La entidad es Usuario. Tendrá un método como puedeCrearSala() que revisará cuántas salas tiene actualmente con el rol MASTER. Si el conteo es 2, arroja un error.

El Use Case que la ejecuta:
El Use Case es CrearNuevaSalaUseCase.

Entrada: usuarioId y nombreSala.

Salida: Los datos de la nueva Sala (incluyendo su código generado).

Pasos: 
1) Obtener al Usuario a través del repositorio.
2) 2) Validar si el usuario puede crear una sala más (Regla 3).
3) Generar el código de acceso único.
4) Instanciar la nueva Sala.
5) Crear la relación UsuarioSala como MASTER.
6) Guardar todo a través de los repositorios.

La interfaz del Repository:
Se necesitan dos: IUsuarioRepository (para traer al usuario y sus salas) y ISalaRepository (para guardar la nueva sala y el código generado). 
Podría haber un método como contarSalasDeUsuario(Long usuarioId).

El Adapter que la implementaría:
Un adaptador de BD conectado a PostgreSQL.


=
**PARTE 5. Cierre**
=

• ¿Qué parte de su proyecto les costó más ubicar en una capa?

cualquiera de dominio ya que se sentia como que si la ubicabas mal en una subcarpeta podrias terminar muy enredado, de hecho ni siquiera estoy seguro de que las este 
acomodando bien tbh

• ¿Qué ganan con poder cambiar la infraestructura sin tocar el núcleo?
Si una tecnología queda obsoleta en el futuro, el núcleo del negocio permanece intacto.
Los cambios tecnológicos se limitan estrictamente a la capa más reduciendo el riesgo de que una actualización 
rompa las reglas contables o de convivencia de las viviendas compartidas.

<img width="2746" height="4190" alt="Christmas Shopping Decision Tree-2026-10-15-043011" src="https://github.com/user-attachments/assets/8e016cf7-168b-430e-a485-5f196470c3da" />

