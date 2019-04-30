package com.elastisys.autoscaler.core.metronome.impl.standard.config;

import static com.elastisys.scale.commons.util.precond.Preconditions.checkArgument;

import java.util.Objects;
import java.util.Optional;

import com.elastisys.autoscaler.core.autoscaler.AutoScaler;
import com.elastisys.autoscaler.core.cloudpool.api.CloudPoolProxy;
import com.elastisys.autoscaler.core.metronome.api.Metronome;
import com.elastisys.autoscaler.core.metronome.impl.standard.StandardMetronome;
import com.elastisys.autoscaler.core.prediction.api.PredictionSubsystem;
import com.elastisys.scale.commons.json.JsonUtils;
import com.elastisys.scale.commons.json.types.TimeInterval;

/**
 * The configuration class of the {@link StandardMetronome}.
 *
 * @see StandardMetronome
 *
 */
public class StandardMetronomeConfig {

    /** Default value for {@link #interval}. */
    public static final TimeInterval DEFAULT_METRONOME_INTERVAL = TimeInterval.seconds(30L);
    /** Default value for {@link #logOnly}. */
    public static final Boolean DEFAULT_LOG_ONLY = false;

    /**
     * The <i>prediction horizon</i> to use for predictions. That is, how far
     * into the future the {@link Metronome} will ask the
     * {@link PredictionSubsystem} to look. Typically, you want to set this
     * close to the boot-time of a new instance so that predictors can provision
     * instances in a proactive manner, such that they are fully operational
     * when the predicted need arises. In case of long boot time, this value can
     * be set to a lower value to prevent overly aggressive scaling.
     */
    private final TimeInterval horizon;
    /**
     * The interval between two resize iterations in seconds. May be
     * <code>null</code>. Default: {@link #DEFAULT_METRONOME_INTERVAL}.
     */
    private final TimeInterval interval;
    /**
     * {@code true} if the {@link AutoScaler} is in <i>log-only mode</i>,
     * <code>false</code> otherwise. May be <code>null</code>. Default:
     * <code>false</code>.
     * <p/>
     * When in log-only mode, the {@link AutoScaler} only outputs pool size
     * predictions (as events on the event bus) but does not ask the
     * {@link CloudPoolProxy} to carry out the resize operations.
     */
    private final Boolean logOnly;

    /**
     * Constructs a new {@link StandardMetronomeConfig}.
     *
     * @param horizon
     *            The prediction horizon in seconds.
     * @param interval
     *            The interval between two resize iterations in seconds. May be
     *            <code>null</code>. Default:
     *            {@link #DEFAULT_METRONOME_INTERVAL}.
     * @param logOnly
     *            {@code true} if the {@link AutoScaler} is in <i>log-only
     *            mode</i>, <code>false</code> otherwise. When in log-only mode,
     *            the {@link AutoScaler} only outputs pool size predictions (as
     *            events on the event bus) but does not ask the
     *            {@link CloudPoolProxy} to carry out the resize operations. May
     *            be <code>null</code>. Default: <code>false</code>.
     */
    public StandardMetronomeConfig(TimeInterval horizon, TimeInterval interval, Boolean logOnly) {
        this.horizon = horizon;
        this.interval = interval;
        this.logOnly = logOnly;
    }

    /**
     * Returns the prediction horizon in seconds.
     *
     * @return
     */
    public TimeInterval getHorizon() {
        return this.horizon;
    }

    /**
     * Returns the interval between two resize iterations in seconds.
     *
     * @return
     */
    public TimeInterval getInterval() {
        return Optional.ofNullable(this.interval).orElse(DEFAULT_METRONOME_INTERVAL);
    }

    /**
     * Returns {@code true} if the {@link AutoScaler} is in <i>log-only
     * mode</i>, <code>false</code> otherwise.
     * <p/>
     * When in log-only mode, the {@link AutoScaler} only outputs pool size
     * predictions (as events on the event bus) but does not ask the
     * {@link CloudPoolProxy} to carry out the resize operations.
     *
     * @return
     */
    public boolean isLogOnly() {
        return Optional.ofNullable(this.logOnly).orElse(DEFAULT_LOG_ONLY);
    }

    public void validate() throws IllegalArgumentException {
        checkArgument(this.horizon != null, "metronome: missing horizon");
        checkArgument(this.horizon.getSeconds() > 0, "metronome: horizon needs to be a positive duration");
        checkArgument(getInterval().getSeconds() > 0, "metronome: interval needs to be a positive duration");
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.horizon, this.interval, this.logOnly);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StandardMetronomeConfig) {
            StandardMetronomeConfig that = (StandardMetronomeConfig) obj;
            return Objects.equals(this.horizon, that.horizon) //
                    && Objects.equals(this.interval, that.interval) //
                    && Objects.equals(this.logOnly, that.logOnly);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
