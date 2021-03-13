import React from 'react';
import './App.css';
import {QueryClient, QueryClientProvider} from "react-query";
import Search from "./search/Search";

const queryClient = new QueryClient();

function App() {
  return (
      <QueryClientProvider client={queryClient}>
        <Search/>
      </QueryClientProvider>
  );
}

export default App;
