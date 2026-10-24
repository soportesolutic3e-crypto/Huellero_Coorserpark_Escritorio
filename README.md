# 🖐️ COORSERPARK — Sistema de Control de Huellas v23.0

> **Sistema biométrico de control de asistencia desarrollado por Solutic3E, ahora con conexión automática a una IP principal y una alterna.**

---

## ¿Qué es esto?

**COORSERPARK Control de Huellas** es una aplicación de escritorio que permite a las empresas registrar la entrada y salida de sus empleados usando **huella dactilar o tarjeta RFID**, sin depender de internet en tiempo real. Los datos se guardan en una base local y se sincronizan automáticamente con un servidor central cuando hay conexión disponible.

Desarrollado y mantenido por **Solutic 3E**. La **versión 23** conserva todo lo de la v22 (interfaz moderna, licencia por equipo, ofuscación) y agrega **failover automático de servidor**: si la IP principal falla, el huellero sigue funcionando por la IP alterna sin intervención del usuario.

---

## 🆕 Novedades de la versión 23

### 🌐 IP principal y alterna con cambio automático
- La pantalla **Configurar** ahora tiene dos campos: **IP PORT** (principal) e **IP ALTERNA**.
- Si una petición falla por la ruta activa, el sistema la **reintenta de inmediato por la otra ruta**, dentro de la misma operación.
- Mientras la alterna esté funcionando, **cada 5 minutos vuelve a probar la principal** y regresa a ella sola cuando se recupera.
- La configuración se lee en cada petición, así que un cambio de IP se aplica **sin reiniciar** la aplicación.

### 🛡️ Protección contra registros duplicados
Los servicios que **escriben** en el servidor (eventos, vales, registro de licencia) solo se reintentan por la otra ruta si el fallo ocurrió **antes de terminar de enviar la petición**. Así nunca se duplica una marcación ni un vale por reintentar algo que el servidor pudo haber procesado.

### ⏱️ Timeouts diferenciados
| Tipo de llamada | Conexión | Lectura |
|---|---|---|
| **En vivo** (lectura de huella/tarjeta) | 2.5 s | 5 s |
| **Sincronización y administración** | 3.5 s | 10 s |

El escaneo en tiempo real responde rápido aunque un servidor esté caído, y la sincronización de fondo tiene más margen.

### 🧵 Corrección de hilos en el ingreso por tarjeta
Ajustes de concurrencia en `IngresoModelTarjeta` para que la lectura de tarjeta no bloquee ni se cruce con otras tareas.

### 🏢 Sede guardada en la configuración
Al guardar la configuración, el `id_sede` seleccionado también se almacena en la tabla `config` local.

### 🖨️ Versión en el vale
El pie del vale impreso ahora muestra **V23.0.0**, útil para soporte.

### 🔢 Versión interna
`versionactualapp`, `versioninstalacion` y la sede por defecto pasan a **23.0.0**.

---

## ✨ ¿Qué puede hacer?

| Función | Descripción |
|---|---|
| 🖐️ **Registro biométrico** | Identifica empleados por huella dactilar en tiempo real |
| 💳 **Registro por tarjeta** | Soporte para tarjetas RFID como alternativa a la huella |
| 🔄 **Sincronización automática** | Se conecta al servidor y actualiza la base de datos local al iniciar |
| 🌐 **Failover de servidor** | Cambia solo entre IP principal y alterna según cuál responda |
| 👤 **Enrolamiento** | Permite registrar hasta 10 huellas por empleado |
| 🖨️ **Impresión de vales** | Genera e imprime comprobantes de asistencia automáticamente |
| 🏢 **Multi-sede** | Soporta múltiples sedes configurables desde la app |
| 🔐 **Licencia por equipo** | El sistema queda amarrado al equipo donde se instala, evitando copias no autorizadas |

---

## 🖥️ Pantallas principales

### Pantalla de ingreso
Muestra en tiempo real el nombre, foto (huella), cargo, empresa y jornada laboral del empleado al momento de registrar entrada o salida.

### Sincronización
Al iniciar, el sistema sincroniza los datos del servidor con la base de datos local. Funciona aunque no haya internet durante el día.

### Enrolamiento biométrico
Interfaz para registrar las huellas de nuevos empleados. Muestra visualmente cuáles huellas están registradas y cuáles están vacías.

### Configuración
Permite seleccionar la sede activa y configurar la **IP principal** y la **IP alterna** del servidor.

---

## ⚙️ Tecnologías utilizadas

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 8 |
| Interfaz gráfica | Java Swing + FlatLaf 3.5.4 |
| Base de datos local | SQLite |
| Backend / Web services | PHP con ScriptCase |
| Lector biométrico | DigitalPersona OneTouch SDK |
| IDE | NetBeans (build con Ant) |
| Seguridad | AES-128 + SHA-256 + ProGuard |

---

## 🔐 Sistema de licencias

Cada instalación queda **amarrada al equipo** donde se instala. Si alguien intenta copiar el sistema a otro computador, la aplicación detecta el cambio y bloquea el acceso mostrando qué componente no coincide.

La licencia se genera automáticamente en la primera ejecución usando:
- Serial del disco duro
- ID del procesador
- Un código único (salt) generado en esa instalación

> Desde la v22 **ya no se usa la MAC** de la tarjeta de red, para que cambiar o desactivar un adaptador no invalide la licencia. Las licencias antiguas (formato con MAC) siguen siendo válidas: la MAC simplemente se ignora al comparar.

---

## 🚀 Requisitos para instalar

- Windows 10 / 11
- Java 8 o superior instalado
- Lector biométrico DigitalPersona U.are.U
- Conexión al servidor (principal o alterna) para la sincronización inicial; luego funciona offline

---

## 📁 Estructura del proyecto

```
src/
├── controlingreso/     # Punto de entrada de la aplicación
├── vistas/             # Pantallas (Inicio, Ingreso, IngresoTarjeta, Login, Configurar, NuevoUsuario)
├── models/             # Lógica de negocio de cada pantalla
├── database/           # Conexión y utilidades SQLite
├── process/            # Sincronización, web services (JSONTask con failover), licencia
└── devices/            # Integración con lector biométrico
```

---

## 👨‍💻 Desarrollado por

**Solutic3E - Transformando Ideas**
Sistema de control biométrico empresarial — Versión 23.0

---

> *Este software es de uso privado y está protegido por licencia de equipo. Su copia, redistribución o uso no autorizado está prohibido.*
