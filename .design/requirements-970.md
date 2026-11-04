# Requirements for a Delegation Hook in the OSGi Framework

- **Issue**: [osgi/osgi#970](https://github.com/osgi/osgi/issues/970)
- **Related**: [osgi/osgi#372](https://github.com/osgi/osgi/issues/372) (Java SPI support), Core Framework chapter 3.9 (class loading), Weaving Hook Service Specification
- **Prior work**: the internal `ClassLoaderHook` of Eclipse Equinox, used by its Service Loader Mediator implementation (`org.eclipse.equinox.spi`)

## Terminology

- **Search Order** -
  The sequence of steps a bundle class loader follows to find a class or resource, see the Overall Search Order in the Core Framework specification.

- **Asking Bundle** -
  The bundle whose class loader was asked for a class or resource.

- **Delegation Hook** -
  A service registered by a bundle that is consulted by the framework to contribute a class or resource after the Search Order did not provide it.

- **Provider Bundle** -
  A bundle whose class loader supplies a class to an Asking Bundle on behalf of a Delegation Hook.

## Problem Description

The Search Order is closed.
A bundle can see exactly what its wiring, its class path and its dynamic imports give it.
Extensions that need to make more visible to a bundle, without that bundle declaring anything, have no specified way to do so.

Every such extension today uses a framework specific mechanism:

- **Bytecode weaving** rewrites call sites of the bundles that use the extended feature.
  It is fragile against new Java versions and language constructs.
- **Framework internals** such as the Equinox `ClassLoaderHook` are only reachable at framework construction time, and are not portable to other frameworks.
- **Launcher configuration** such as the Felix `felix.bootdelegation.classloaders` can only be applied by whoever launches the framework.

The Service Loader Mediator in Equinox shows that two interception points are enough for a rich feature: answer a resource request with resources of other bundles, and answer a class load miss with a class from another bundle.
Nothing in that feature is specific to `java.util.ServiceLoader`.
The same two points serve, for example, supplementer bundles, compatibility shims for renamed packages and resource overlays.

## Use Cases

### UC1: Java SPI Across Bundles

A consumer bundle calls `ServiceLoader.load(Greeter.class, loader)` with its own class loader.
A provider bundle ships `META-INF/services/org.example.Greeter` and the implementation class in a package it does not export.
An extension bundle makes the file visible to the consumer and loads the named class from the provider bundle.
Neither bundle carries OSGi specific metadata, and the extension does not weave any bundle.

### UC2: Extension Active Before Any Bundle Starts

The extension of UC1 is installed as a framework extension bundle.
It must be able to take part in class loading from the first class load of the first bundle, independent of the launcher.

### UC3: Compatibility Shim

A library renamed a package.
Bundles that still use the old name get the classes of the new package from a shim bundle, without the shim exporting the old package.

### UC4: Resource Overlay

A bundle asks for `META-INF/overlay/config.properties`.
The framework finds the bundle's own file.
An extension additionally contributes the files of other bundles, in a defined order.

### UC5: Wiring Stays Authoritative

A bundle imports `org.example.api`.
An extension must not be able to replace, hide or add to what the import provides.

### UC6: Provider Is Refreshed

The provider bundle of UC1 is updated and refreshed.
A class from the old provider revision must not stay in use by the consumer.

## Requirements

### R1: Additive Extension Point

The framework must provide a service based extension point that is consulted after the Search Order found nothing.
It must only be able to add classes and resources, never to replace or hide what the Search Order finds.

### R2: Classes, Resources and Resource Enumerations

The extension point must cover the three requests a bundle class loader answers: a single class, a single resource and the enumeration of all resources with a name.
For an enumeration the contributed resources must be appended to the resources the Search Order found.

### R3: Wiring Remains Authoritative

The extension point must not be consulted for `java.*` packages, nor for packages the Asking Bundle imports, requires or exports.

### R4: Multiple Hooks, Defined Order

Multiple hooks must be possible.
They must be consulted in service ranking order.
For a single class or resource the first result wins; for an enumeration all results are appended.

### R5: Refresh Dependency

If a class supplied by a hook was defined by the class loader of another bundle, the Asking Bundle must depend on that Provider Bundle for the purposes of refreshing and of the dependency closure, like it does for a dynamic import.

### R6: Robust Invocation

The framework must not hold its locks while it calls a hook.
A hook must not be called recursively for the same bundle wiring and name on the same thread.
A hook that throws must not break class loading: a framework error event must be published and the request continues as if the hook returned nothing.

### R7: Security

Registering a hook must require a permission.
Classes and resources supplied by a hook remain subject to the permissions of the Asking Bundle.

### R8: No Impact Without Hooks

Class and resource loading must be unchanged while no hook is registered.
The extension point must not add cost to successful lookups.

### R9: Available As Framework Extension

The extension point must be usable by a framework extension bundle that registers the service from its `ExtensionBundle-Activator`, as well as by an ordinary bundle.
No framework specific launcher configuration may be needed.

### R10: Technique Neutrality

The extension point must be defined by what the framework consults and what the hook may answer.
It must not prescribe how a hook finds its answer, and it must not require the framework to inspect call stacks or the thread context class loader.

## Out of Scope

- The observable behavior of Java SPI support in the framework (osgi/osgi#372) and a property to switch it off.
- Determining the calling bundle of a request that does not arrive through a bundle class loader, for example `ServiceLoader.load(Class)` with the thread context class loader.
  The core specification does not define the thread context class loader, and a shared one cannot select a provider per caller.
- Hooks that run before the Search Order, as in the internal Equinox `pre` points.
