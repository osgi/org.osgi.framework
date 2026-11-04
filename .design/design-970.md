# Design: Delegation Hook

Addresses [requirements-970.md](requirements-970.md).

## Requirements

See `requirements-970.md`, R1 to R10.

## Technical Solution

### Overview

A new package `org.osgi.framework.hooks.delegation` (version 1.0) contains one `@ConsumerType` service interface, `DelegationHook`.
Bundles register it with the service registry.
The framework calls it from the bundle class loader as an additional last step of the Overall Search Order.

```
loadClass / getResource / getResources
        |
   Search Order (java.*, boot delegation, imports, required bundles,
                 class path, dynamic imports)
        |
   found?  --yes--> return (getResources: continue to hooks and append)
        |no
   DelegationHook services, in ranking order
```

### API

```java
@ConsumerType
public interface DelegationHook {
	Class<?> findClass(BundleWiring wiring, String name) throws ClassNotFoundException;
	URL findResource(BundleWiring wiring, String name);
	Collection<URL> findResources(BundleWiring wiring, String name);
}
```

- `wiring` is the wiring of the Asking Bundle, which gives the hook access to the bundle, its class loader, its capabilities and its wires.
- `findClass` returns the class or `null`.
  The class may be defined by any class loader.
- `findResource` returns a URL or `null`.
- `findResources` returns the URLs to append, or `null` or an empty collection.

### When the framework calls the hook

- `findClass` and `findResource` are called only if the Search Order found nothing.
- `findResources` is called always, and the result is appended after the resources of the Search Order.
- No call is made for names in `java.*`.
- No call is made when the package of the name is imported, required, or exported by the Asking Bundle, because wiring is authoritative.
  For a resource the package is derived from the path, like it is for dynamic imports.
- Hooks are called in service ranking order.
  The first non-null result of `findClass` and `findResource` wins.
- A hook is not called again for the same wiring and name on the same thread while it is running.

### Failure and isolation

- The framework calls hooks without holding its own locks, in a privileged block.
- A hook that throws is ignored for this request and a `FrameworkEvent.ERROR` is published.
  The framework should not call that registration again, like weaving hooks.

### Refresh dependency

If `findClass` returns a class whose class loader belongs to another bundle revision, the framework records the Asking Bundle as dependent on that provider.
A refresh of the provider then includes the Asking Bundle in the dependency closure, like a dynamic import does.
No wire is created.

### Deployment

The hook is an ordinary service.
A framework extension bundle may register it from its `ExtensionBundle-Activator`, so it is active before the first bundle is started.

### Security

Registering the service requires `ServicePermission[DelegationHook, REGISTER]`.
The framework is the only consumer of the service and needs no `GET` permission.
Defined classes remain subject to the permissions of the bundle that provided them.

## Alternatives

- **Hooks before the Search Order** (Equinox `preFindClass`).
  Can hide what the wiring provides, so they break the module layer and are rejected.
- **Weaving**.
  Does not help for resources, and requires every consumer to be woven.
- **Adding a wire** for classes obtained through the hook.
  Gives visibility in the wiring API and a standard refresh behavior, but a wire needs a capability on the provider side, and the packages in question are by definition not exported.
  This is an open point; the design uses a plain dependency.
- **Passing the caller or thread context class loader to the hook**.
  Dropped, see Out of Scope in the requirements.

## Data Transfer Objects

None.

## Open Points

- Wire versus plain dependency for provider classes, see above.
- Whether the hook should be called for `findResources` if the package is exported by the Asking Bundle but the resource is not in it.
- The chapter number of the new specification, 58 is a placeholder.
