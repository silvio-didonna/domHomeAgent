#include <OneWire.h>

// OneWire DS18S20, DS18B20, DS1822 Temperature Example
//
// http://www.pjrc.com/teensy/td_libs_OneWire.html
//
// The DallasTemperature library can do all this work for you!
// http://milesburton.com/Dallas_Temperature_Control_Library

OneWire  ds(10);  // on pin 10 (a 4.7K resistor is necessary)
String inputString = "";         // a string to hold incoming data
boolean stringComplete = false;  // whether the string is complete


void setup(void) {
  Serial.begin(9600);
  // reserve 100 bytes for the inputString:
  inputString.reserve(100);
}

void loop(void) {
  serialEvent(); //call the function
  // print the string when a newline arrives:
  if (stringComplete) {
    if (inputString.equals("therm1\n")) {
      float temp=getTemperature();
      Serial.println(temp, 4);
    }
    // clear the string:
    inputString = "";
    stringComplete = false;
  }
}

/*
  SerialEvent occurs whenever a new data comes in the
 hardware serial RX.  This routine is run between each
 time loop() runs, so using delay inside loop can delay
 response.  Multiple bytes of data may be available.
 */
void serialEvent() {
  while (Serial.available()) {
    // get the new byte:
    char inChar = (char)Serial.read();
    // add it to the inputString:
    inputString += inChar;
    // if the incoming character is a newline, set a flag
    // so the main loop can do something about it:
    if (inChar == '\n') {
      stringComplete = true;
    }
  }
}

float getTemperature() {
  byte i;
  byte present = 0;
  byte type_s;
  byte data[12];
  byte addr[8];
  float celsius;// fahrenheit;

//28 FF C5 1F 12 14 0 C8
addr[0]=0x28;
addr[1]=0xFF;
addr[2]=0xC5;
addr[3]=0x1F;
addr[4]=0x12;
addr[5]=0x14;
addr[6]=0x0;
addr[7]=0xC8;
  /*
  if ( !ds.search(addr)) {
    //Serial.println("No more addresses.");
    //Serial.println();
    ds.reset_search();
    delay(250);
    return 0;
  }
  
  Serial.print("ROM =");
  for( i = 0; i < 8; i++) {
    Serial.write(' ');
    Serial.print(addr[i], HEX);
  }
*/
  if (OneWire::crc8(addr, 7) != addr[7]) {
      //Serial.println("CRC is not valid!");
      return 0;
  }
  //Serial.println();
 /*
  // the first ROM byte indicates which chip
  switch (addr[0]) {
    case 0x10:
      //Serial.println("  Chip = DS18S20");  // or old DS1820
      type_s = 1;
      break;
    case 0x28:
      //Serial.println("  Chip = DS18B20");
      type_s = 0;
      break;
    case 0x22:
      //Serial.println("  Chip = DS1822");
      type_s = 0;
      break;
    default:
      //Serial.println("Device is not a DS18x20 family device.");
      return 0;
  } 
  */

  ds.reset();
  ds.select(addr);
  ds.write(0x44, 1);        // start conversion, with parasite power on at the end
  
  delay(1000);     // maybe 750ms is enough, maybe not
  // we might do a ds.depower() here, but the reset will take care of it.
  
  present = ds.reset();
  ds.select(addr);    
  ds.write(0xBE);         // Read Scratchpad

  //Serial.print("  Data = ");
  //Serial.print(present, HEX);
  //Serial.print(" ");
  for ( i = 0; i < 9; i++) {           // we need 9 bytes
    data[i] = ds.read();
    //Serial.print(data[i], HEX);
    //Serial.print(" ");
  }
  //Serial.print(" CRC=");
  //Serial.print(OneWire::crc8(data, 8), HEX);
  //Serial.println();

  // Convert the data to actual temperature
  // because the result is a 16 bit signed integer, it should
  // be stored to an "int16_t" type, which is always 16 bits
  // even when compiled on a 32 bit processor.
  int16_t raw = (data[1] << 8) | data[0];
  /*
  if (type_s) {
    raw = raw << 3; // 9 bit resolution default
    if (data[7] == 0x10) {
      // "count remain" gives full 12 bit resolution
      raw = (raw & 0xFFF0) + 12 - data[6];
    }
  } else {
  */
    byte cfg = (data[4] & 0x60);
    // at lower res, the low bits are undefined, so let's zero them
    if (cfg == 0x00) raw = raw & ~7;  // 9 bit resolution, 93.75 ms
    else if (cfg == 0x20) raw = raw & ~3; // 10 bit res, 187.5 ms
    else if (cfg == 0x40) raw = raw & ~1; // 11 bit res, 375 ms
    //// default is 12 bit resolution, 750 ms conversion time
 // }
  celsius = (float)raw / 16.0;
  //fahrenheit = celsius * 1.8 + 32.0;
  //Serial.print("  Temperature = ");
  //Serial.print(celsius);
  //Serial.print(" Celsius, ");
  //Serial.print(fahrenheit);
  //Serial.println(" Fahrenheit");

  return celsius;
}
