# 🚀 Resumen del Proyecto: POC Spring Batch + Kafka + NATS
**Sistema de Compensación y Liquidación de Alta Velocidad**

Este documento resume la trayectoria técnica, las decisiones arquitectónicas y los detalles finales de la implementación de esta Prueba de Concepto (POC), diseñada para simular una cámara de compensación financiera de próxima generación.

---

## 1. 🎯 El Desafío
Diseñar un sistema capaz de manejar **transacciones financieras de alta velocidad** equilibrando dos necesidades contrapuestas:
1.  **Validación de ultra baja latencia** (Chequeos de fraude < 50ms).
2.  **Procesamiento robusto y duradero** (Liquidación garantizada).

## 2. 🏗️ La Solución: Arquitectura Híbrida Orientada a Eventos (EDA)

Implementamos una **EDA Híbrida** que divide el flujo en un "Camino Caliente" (Velocidad) y un "Camino Frío" (Confiabilidad).

### ⚡ El "Camino Caliente" (Sincrónico)
- **Tecnología**: **NATS** (Patrón Request-Reply).
- **Rol**: Detección de Fraude en Tiempo Real.
- **¿Por qué?**: A diferencia de Kafka, NATS es efímero y ligero, perfecto para verificar "¿Es esta transacción válida ahora mismo?" sin la sobrecarga de escritura en disco.
- **Resultado**: Latencia de ~1ms para decisiones de fraude.

### 🧊 El "Camino Frío" (Asincrónico)
- **Tecnología**: **Apache Kafka**.
- **Rol**: Fuente de Verdad y Durabilidad.
- **¿Por qué?**: Una vez aprobada, la transacción *no debe perderse*. Kafka garantiza la durabilidad y el orden (particionando por ID de Banco).
- **Resultado**: Las transacciones aprobadas se almacenan de forma segura incluso si el sistema de liquidación está fuera de línea.

### 📦 El Motor de Liquidación (Settlement Engine)
- **Tecnología**: **Spring Batch** + **Base de Datos H2**.
- **Rol**: Procesamiento al final del día (o micro-batches).
- **¿Por qué?**: El procesamiento de archivos de pago requiere transacciones ACID y grandes volúmenes. Spring Batch gestiona bloques (chunks), reintentos y generación de archivos de forma robusta.

---

## 3. 🛠️ Detalles de Implementación

### A. Infraestructura (Podman/Docker)
- **Kafka + Zookeeper**: La columna vertebral de mensajería.
- **NATS Server**: Nervio de mensajería de alto rendimiento.
- **Microservicios**: 3 aplicaciones Spring Boot corriendo en paralelo.

### B. Mapa de Microservicios
| Servicio | Stack Tecnológico | Responsabilidad |
|---------|-------------------|-----------------|
| **Payment Gateway** | Spring Boot Web, NATS Client, Kafka Producer, Vue.js | Acepta peticiones API, orquestar validaciones, actualiza la UI. |
| **Fraud Stub** | NATS Listener (Sincrónico) | Simula reglas de negocio bloqueantes (límites de monto, países bloqueados). |
| **Settlement Batch** | Kafka Consumer, Spring Batch, JPA | Consume eventos, los almacena en DB para procesamiento batch y genera archivos CSV. |

### C. El Dashboard de Monitoreo
Un "Centro de Comando" en tiempo real construido con **Vue.js 3 + Tailwind CSS** (sin necesidad de compilación npm), servido directamente por Spring Boot:
- **WebSockets (STOMP)**: Transmite logs y métricas desde el Servidor → Cliente.
- **Simulador de Tráfico**: Un botón que dispara peticiones asíncronas concurrentes para probar el flujo bajo carga.
- **Visuales**: Indicadores de latencia, flujo de eventos de Kafka en vivo y una terminal de logs estilo Linux.

---

## 4. 🎓 Aprendizajes Clave y Patrones Utilizados

1.  **Request-Reply con NATS**: Cómo conectar una API REST sincrónica con el mundo de mensajería asíncrona sin bloquear hilos innecesariamente.
2.  **Estrategia de Doble Broker**: Usar la herramienta adecuada para cada tarea (NATS para velocidad, Kafka para almacenamiento persistente).
3.  **Patrón Stream-to-Batch**: Almacenar datos de un flujo (Kafka) en una tabla de base de datos antes de un procesamiento por lotes, patrón común en procesos ETL financieros.
4.  **Monitoreo Full-Stack**: Integración de logs estándar de Java (`Logback`) con WebSockets para crear una experiencia de "Live Tail" en el navegador.

---

## 5. ✅ Estado Final
El sistema está **completamente operativo** ejecutándose localmente.

- [x] **Infraestructura**: Podman Compose arriba y funcional.
- [x] **Backend**: Los 3 servicios compilados y comunicándose.
- [x] **Frontend**: Dashboard visualizando métricas en tiempo real.
- [x] **Tráfico**: Simulador generando exitosamente transacciones aprobadas y rechazadas.

### Próximos Pasos para Producción 🚀
- Reemplazar H2 con **PostgreSQL**.
- Habilitar **SSL/TLS** para NATS y Kafka.
- Desplegar en **Kubernetes** con charts de Helm.
- Reemplazar los stubs de fraude con modelos reales de ML.

---
*Construido por Antigravity y Sesión de Pair Programming del Usuario [Camilo Ñustes @cnustes] - Enero 2026*
