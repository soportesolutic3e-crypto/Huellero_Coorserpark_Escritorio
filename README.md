# 🖐️ CORSERPARK — Sistema de Control de Huellas v21.0

> **La versión más estable y completa del sistema biométrico de control de asistencia desarrollado por Solutic 3E.**

---

## ¿Qué es esto?

**CORSERPARK Control de Huellas** es una aplicación de escritorio que permite a las empresas registrar la entrada y salida de sus empleados usando **huella dactilar o tarjeta RFID**, sin depender de internet en tiempo real. Los datos se sincronizan automáticamente con un servidor central cuando hay conexión disponible.

Desarrollado y mantenido por **Solutic 3E**, este sistema lleva más de 20 versiones de evolución y la **versión 21** representa su estado más maduro, estable y visualmente moderno.

---

## ✨ ¿Qué puede hacer?

| Función | Descripción |
|---|---|
| 🖐️ **Registro biométrico** | Identifica empleados por huella dactilar en tiempo real |
| 💳 **Registro por tarjeta** | Soporte para tarjetas RFID como alternativa a la huella |
| 🔄 **Sincronización automática** | Se conecta al servidor y actualiza la base de datos local al iniciar |
| 👤 **Enrolamiento** | Permite registrar hasta 10 huellas por empleado |
| 🖨️ **Impresión de vales** | Genera e imprime comprobantes de asistencia automáticamente |
| 🏢 **Multi-sede** | Soporta múltiples sedes configurables desde la app |
| 🔐 **Licencia por equipo** | El sistema queda amarrado al hardware donde se instala, evitando copias no autorizadas |

---

## 🖥️ Pantallas principales

### Pantalla de ingreso
Muestra en tiempo real el nombre, foto (huella), cargo, empresa y jornada laboral del empleado al momento de registrar entrada o salida.

### Sincronización
Al iniciar, el sistema sincroniza automáticamente los datos del servidor con la base de datos local. Funciona aunque no haya internet durante el día.

### Enrolamiento biométrico
Interfaz para registrar las huellas de nuevos empleados. Muestra visualmente cuáles huellas están registradas y cuáles están vacías.

### Configuración
Permite seleccionar la sede activa y configurar la dirección del servidor.

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

Cada instalación queda **amarrada al hardware del equipo** donde se instala. Si alguien intenta copiar el sistema a otro computador, la aplicación detecta el cambio y bloquea el acceso mostrando el detalle exacto de qué componente de hardware no coincide.

La licencia se genera automáticamente en la primera ejecución usando:
- MAC address de la tarjeta de red
- Serial del disco duro
- ID del procesador
- Un código único irrepetible generado en esa instalación

---

## 🚀 Requisitos para instalar

- Windows 10 / 11
- Java 8 o superior instalado
- Lector biométrico DigitalPersona U.are.U
- Conexión a internet para sincronización inicial (luego funciona offline)

---

## 📁 Estructura del proyecto

```
src/
├── controlingreso/     # Punto de entrada de la aplicación
├── vistas/             # Pantallas (Inicio, IngresoTarjeta, Login, Configurar, NuevoUsuario)
├── models/             # Lógica de negocio de cada pantalla
├── database/           # Conexión y utilidades SQLite
├── process/            # Sincronización, web services, licencia
└── devices/            # Integración con lector biométrico
```

---

## 👨‍💻 Desarrollado por

**Solutic 3E**
Sistema de control biométrico empresarial — Versión 21.0 (2025)

---

> *Este software es de uso privado y está protegido por licencia de hardware. Su copia, redistribución o uso no autorizado está prohibido.*
"# Huellero_Coorserpark_Escritorio" 
"# Huellero_Coorserpark_Escritorio" 
"# Huellero_Coorserpark_Escritorio" 
