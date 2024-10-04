package org.finos.legend.engine.plan.execution.stores.deephaven.connection;

public interface DeephavenSessionProvider extends LegendModuleSpecificExtension
{
    static List<DeephavenSessionProvider> providers()
    {
        MutableList<DeephavenSessionProvider> providers = Lists.mutable.empty();
        ServiceLoader.load(DeephavenSessionProvider.class).iterator().forEachRemaining(providers::add);
        return providers.asUnmodifiable();
    }
}
