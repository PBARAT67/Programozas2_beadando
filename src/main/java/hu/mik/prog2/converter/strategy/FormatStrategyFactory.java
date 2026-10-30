package hu.mik.prog2.converter.strategy;

import hu.mik.prog2.converter.exception.UnsupportedFormatException;
import hu.mik.prog2.converter.model.FormatType;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Factory class implementing the Factory Method pattern for instantiating
 * and managing FormatStrategy instances based on format types or file paths.
 */
public class FormatStrategyFactory {

    private final Map<FormatType, FormatStrategy> strategyRegistry;

    public FormatStrategyFactory() {
        this.strategyRegistry = new EnumMap<>(FormatType.class);
        registerDefaultStrategies();
    }

    private void registerDefaultStrategies() {
        registerStrategy(new CsvFormatStrategy());
        registerStrategy(new JsonFormatStrategy());
        registerStrategy(new XmlFormatStrategy());
    }

    /**
     * Registers a strategy for its corresponding FormatType.
     */
    public void registerStrategy(FormatStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy cannot be null");
        strategyRegistry.put(strategy.getFormatType(), strategy);
    }

    /**
     * Retrieves the format strategy for a given FormatType.
     *
     * @param formatType the requested format type
     * @return the matching FormatStrategy instance
     * @throws UnsupportedFormatException if no strategy is registered for the type
     */
    public FormatStrategy getStrategy(FormatType formatType) throws UnsupportedFormatException {
        if (formatType == null) {
            throw new UnsupportedFormatException("null");
        }
        FormatStrategy strategy = strategyRegistry.get(formatType);
        if (strategy == null) {
            throw new UnsupportedFormatException(formatType.name());
        }
        return strategy;
    }

    /**
     * Factory method resolving and returning strategy based on a file path.
     *
     * @param path the file path whose extension determines the format
     * @return the matching FormatStrategy instance
     * @throws UnsupportedFormatException if format cannot be deduced or is unsupported
     */
    public FormatStrategy getStrategyForPath(Path path) throws UnsupportedFormatException {
        if (path == null) {
            throw new UnsupportedFormatException("null path");
        }
        String fileName = path.getFileName().toString();
        FormatType formatType = FormatType.fromFileNameOrExtension(fileName);
        return getStrategy(formatType);
    }
}
