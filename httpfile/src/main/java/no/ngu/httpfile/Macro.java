package no.ngu.httpfile;

/**
 * The supported macros in the form of {{$macroName [arg1 arg2 ...]}}.
 */
public enum Macro {
  // {{$guid}}
  guid(0),
  // {{$randomInt min max}}
  randomInt(2),
  // {{$timestamp [offset option]}}
  timestamp(0, 2),
  // {{$datetime rfc1123|iso8601 [offset option]}}
  datetime(1, 3),
  // {{$localDatetime rfc1123|iso8601 [offset option]}}
  localDatetime(1, 3),
  // {{$processEnv [%]envVarName}}
  processEnv(1),
  // {{$dotenv [%]variableName}}
  dotenv(1)
  // {{$aadToken [new] [public|cn|de|us|ppe] [<domain|tenantId>] [aud:<domain|tenantId>]}}
  // NYI
  ;

  /**
   * The minimum number of arguments for the macro.
   */
  public final int minArgs;

  /**
   * The maximum number of arguments for the macro.
   */
  public final int maxArgs;

  private Macro(int minArgs, int maxArgs) {
    this.minArgs = minArgs;
    this.maxArgs = maxArgs;
  }
  
  private Macro(int numArgs) {
    this(numArgs, numArgs);
  }
}