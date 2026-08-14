package com.alkacode.crates.animation;

/** Funcoes de easing aplicadas sobre a curva do tempo (0 -> 1). */
public enum Easing {
    LINEAR {
        @Override
        public double apply(double t) {
            return t;
        }
    },
    EASE_IN {
        @Override
        public double apply(double t) {
            return t * t;
        }
    },
    EASE_OUT {
        @Override
        public double apply(double t) {
            return 1 - (1 - t) * (1 - t);
        }
    },
    EASE_IN_OUT {
        @Override
        public double apply(double t) {
            return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
        }
    },
    BOUNCE {
        @Override
        public double apply(double t) {
            double n1 = 7.5625;
            double d1 = 2.75;
            if (t < 1 / d1) {
                return n1 * t * t;
            } else if (t < 2 / d1) {
                return n1 * (t -= 1.5 / d1) * t + 0.75;
            } else if (t < 2.5 / d1) {
                return n1 * (t -= 2.25 / d1) * t + 0.9375;
            } else {
                return n1 * (t -= 2.625 / d1) * t + 0.984375;
            }
        }
    },
    ELASTIC {
        @Override
        public double apply(double t) {
            if (t == 0 || t == 1) {
                return t;
            }
            double c4 = (2 * Math.PI) / 3;
            return Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1;
        }
    };

    public abstract double apply(double t);
}
